import test from 'node:test';
import assert from 'node:assert/strict';
import { parseJson, stringifyJson, queryString, createApi, ApiError, validateBase } from '../src/api.js';
const response = (body, status = 200) => new Response(typeof body === 'string' ? body : JSON.stringify(body), { status });

test('large numeric brand IDs and versions survive read/write without precision loss', () => {
  const data = parseJson('{"id":9223372036854775807,"version":9007199254740993,"price":1999,"text":"id:9223372036854775807 \\\"quoted\\\""}');
  assert.equal(data.id, '9223372036854775807');
  assert.equal(data.version, '9007199254740993');
  assert.equal(data.price, 1999);
  assert.equal(data.text, 'id:9223372036854775807 "quoted"');
  assert.equal(stringifyJson({ brandId: data.id, version: BigInt(data.version) }), '{"brandId":"9223372036854775807","version":9007199254740993}');
  assert.deepEqual(parseJson('{"negative":-9007199254740993,"decimal":1.2,"exponent":1e2}'), { negative: '-9007199254740993', decimal: 1.2, exponent: 100 });
});
test('spec names and repeated values are encoded exactly once', () => {
  const query = queryString({ 'spec.颜色 & 款式': ['红/黑', '100%+蓝'], deleted: false, pageNo: 1, missing: '', absent: null });
  const parsed = new URLSearchParams(query);
  assert.deepEqual(parsed.getAll('spec.颜色 & 款式'), ['红/黑', '100%+蓝']);
  assert.equal(parsed.get('deleted'), 'false');
  assert.equal(parsed.has('missing'), false);
});
test('page envelope and plain user/permission responses remain distinct', async () => {
  const bodies = [ { success: true, records: [{ id: '12' }], page: { total: 1, pageNo: 1, pageSize: 20, pages: 1 } }, { id: '12', status: 'ACTIVE' }, [{ code: 'read', children: [] }], { success: true, data: 'https://cdn.example/a.png' } ];
  const client = createApi({ mode: 'bearer', fetcher: async () => response(bodies.shift()) });
  assert.deepEqual(await client.request('/api/v1/brands'), { rows: [{ id: '12' }], total: 1, pageNo: 1, pageSize: 20, pages: 1 });
  assert.equal((await client.request('/api/users/12')).id, '12');
  assert.ok(Array.isArray(await client.request('/api/users/12/permissions')));
  assert.equal(await client.request('/api/v1/files'), 'https://cdn.example/a.png');
});
test('BFF writes obtain CSRF and preserve raw integer versions with session cookies', async () => {
  const calls = [];
  const client = createApi({ fetcher: async (url, options) => {
    calls.push({ url, ...options });
    return response(url === '/bff/csrf' ? { headerName: 'X-CSRF-TOKEN', token: 'csrf-value' } : { success: true, data: { id: '12' } });
  } });
  await client.request('/api/v1/products/12/approval', { method: 'PUT', body: { version: 9007199254740993n } });
  assert.equal(calls.length, 2);
  assert.equal(calls[0].method, 'GET');
  assert.equal(calls[1].headers['X-CSRF-TOKEN'], 'csrf-value');
  assert.equal(calls[1].body, '{"version":9007199254740993}');
  assert.equal(calls[1].credentials, 'include');
});
test('invalid CSRF prevents the mutation', async () => {
  let calls = 0;
  const client = createApi({ fetcher: async () => { calls++; return response({ token: 'incomplete' }); } });
  await assert.rejects(client.request('/api/users', { method: 'POST', body: {} }), /CSRF/);
  assert.equal(calls, 1);
});
test('Bearer upload keeps multipart boundary under browser control, then token can be cleared', async () => {
  const calls = [];
  const client = createApi({ mode: 'bearer', token: 'test-value', fetcher: async (url, options) => {
    calls.push(options); return response({ success: true, data: true });
  } });
  const body = new FormData(); body.append('file', new Blob(['image']), 'image.png');
  await client.request('/api/v1/files', { method: 'POST', body });
  assert.equal(calls[0].headers.Authorization, 'Bearer test-value');
  assert.equal(calls[0].headers['Content-Type'], undefined);
  assert.equal(calls[0].body, body);
  assert.equal(calls[0].credentials, 'omit');
  client.clearToken(); await client.request('/api/v1/brands');
  assert.equal(calls[1].headers.Authorization, undefined);
});
test('HTTP and business failures include permission, validation and unavailable details', async () => {
  for (const [status, body] of [[401, { code: 'JWT_INVALID', message: '身份已失效' }], [403, { message: '禁止操作' }], [409, { success: false, code: 610200, message: '版本冲突' }], [503, { success: false, code: 610403, message: '搜索未接入' }], [200, { success: false, message: '业务失败', data: { fieldErrors: [{ field: 'name', message: '不能为空' }] } }]]) {
    const client = createApi({ mode: 'bearer', fetcher: async () => response(body, status) });
    await assert.rejects(client.request('/api/v1/products'), error => error instanceof ApiError && error.message === body.message && error.status === status && error.body.code === body.code);
  }
});
test('unknown write outcomes are not retried, including errors during response reading', async () => {
  for (const fetcher of [async () => { throw new Error('offline'); }, async () => ({ text: async () => { throw new Error('reset'); } })]) {
    let calls = 0;
    const client = createApi({ mode: 'bearer', fetcher: (...args) => { calls++; return fetcher(...args); } });
    await assert.rejects(client.request('/api/v1/products', { method: 'POST', body: {} }), /操作结果可能未知/);
    assert.equal(calls, 1);
  }
});
test('HTML fallback is a routing error and a 204 logout succeeds', async () => {
  const bad = createApi({ mode: 'bearer', fetcher: async () => response('<html>login</html>') });
  await assert.rejects(bad.request('/api/v1/brands'), /非 JSON/);
  const good = createApi({ mode: 'bearer', fetcher: async () => new Response(null, { status: 204 }) });
  assert.equal(await good.request('/bff/logout', { method: 'POST' }), null);
});
test('connection URLs reject credentials, unsafe schemes and remote cleartext transport', () => {
  for (const value of ['javascript:alert(1)', 'https://user:password@example.com', 'http://example.com', 'https://example.com/?token=value']) assert.throws(() => validateBase(value));
  assert.equal(validateBase('http://localhost:8080/'), 'http://localhost:8080');
  assert.equal(validateBase('https://example.com/edge/'), 'https://example.com/edge');
});
