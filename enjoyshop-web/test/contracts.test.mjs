import test from 'node:test';
import assert from 'node:assert/strict';
import { id, version, money, editableProduct, validateProduct, productActions, validateImage, newSku } from '../src/contracts.js';
import { searchQuery, searchResults } from '../src/pages/search.js';
import { scheduleBody } from '../src/pages/teaching.js';
import { adBody } from '../src/pages/ads.js';
import { permissionTree } from '../src/pages/users.js';
import { escape, image, safeLink, loadInto, pager } from '../src/ui.js';
import { createApi } from '../src/api.js';

const product = () => ({ spu: { isEnableSpec: false, specItems: {}, paraItems: {} }, skuList: [newSku()] });
test('ID and version bounds reject overflow and keep snowflake precision', () => {
  assert.equal(id('9223372036854775807'), '9223372036854775807');
  assert.equal(version('9007199254740993'), 9007199254740993n);
  assert.equal(version(0), 0n);
  for (const value of ['0', '-1', '1e2', '001', '9223372036854775808', '12a']) assert.throws(() => id(value));
});
test('decimal yuan prices convert to exact integer cents without float rounding', () => {
  assert.equal(money('19.99'), 1999); assert.equal(money('0.29'), 29); assert.equal(money('0'), 0);
  for (const value of ['0.001', '-1', '1e2', '21474836.48']) assert.throws(() => money(value));
});
test('product edit drops derived fields, retains stable SKU IDs and optimistic version', () => {
  const draft = editableProduct({ spu: { id: '10', name: 'Product', version: 8, tenantId: '2', status: '1', isMarketable: false, saleNum: 99 },
    skuList: [{ id: '9007199254740993', spuId: '10', sn: 'A', price: 12, num: 2, saleNum: 200, categoryName: '类目', createTime: 'now' }] });
  assert.equal(draft.version, 8); assert.equal(draft.skuList[0].id, '9007199254740993');
  for (const key of ['id', 'version', 'tenantId', 'status', 'isMarketable', 'saleNum']) assert.equal(Object.hasOwn(draft.spu, key), false);
  for (const key of ['spuId', 'saleNum', 'categoryName', 'createTime']) assert.equal(Object.hasOwn(draft.skuList[0], key), false);
});
test('SKU composition enforces single SKU and matching unique combinations', () => {
  assert.equal(validateProduct(product()).skuList.length, 1);
  const command = product(); command.skuList.push(newSku());
  assert.throws(() => validateProduct(command), /一个 SKU/);
  command.spu.isEnableSpec = true; command.spu.specItems = { 颜色: ['黑', '白'] };
  command.skuList = [{ ...newSku(), id: '11', sn: 'BLACK', spec: { 颜色: '黑' } }, { ...newSku(), id: '12', sn: 'WHITE', spec: { 颜色: '白' } }];
  assert.equal(validateProduct(command).skuList.length, 2);
  command.skuList[1].spec.颜色 = '黑'; assert.throws(() => validateProduct(command), /组合重复/);
  command.skuList[1].spec.颜色 = '红'; assert.throws(() => validateProduct(command), /规格必须/);
  command.skuList[1].spec.颜色 = '白'; command.skuList[1].id = '11'; assert.throws(() => validateProduct(command), /ID 重复/);
  command.skuList[1].id = '12'; command.skuList[1].sn = ' BLACK '; assert.throws(() => validateProduct(command), /条码重复/);
});
test('lifecycle actions match recycle, publication and re-audit restrictions', () => {
  assert.deepEqual(productActions({}, true), ['detail', 'restoration', 'purge']);
  assert.deepEqual(productActions({ isMarketable: true, status: '1' }), ['detail', 'withdrawal']);
  assert.deepEqual(productActions({ isMarketable: false, status: '1' }), ['detail', 'edit', 'publication', 'delete']);
  assert.deepEqual(productActions({ isMarketable: false, status: '0' }), ['detail', 'edit', 'approval', 'delete']);
});
test('search filters preserve long IDs and repeated specs with bounded pagination', () => {
  const values = { keywords: ' 手机 ', categoryId: '9007199254740993', brandId: '', minPrice: '0.29', maxPrice: '100', sort: 'priceAsc' };
  const query = searchQuery(values, { 颜色: ['黑', '白'] }, 500);
  assert.equal(query.categoryId, '9007199254740993'); assert.equal(query.minPrice, 29);
  assert.deepEqual(query['spec.颜色'], ['黑', '白']); assert.equal(query.pageNo, 500);
  assert.throws(() => searchQuery(values, {}, 501), /窗口/);
  assert.throws(() => searchQuery({ ...values, maxPrice: '0.20' }), /最高价格/);
  assert.match(pager({ total: 100000, totalPages: 5000, pageNo: 500, maxAccessiblePageNo: 500 }), /data-page="501" disabled/);
});
test('course schedules keep LocalDateTime whereas advertisements use UTC instants', () => {
  assert.deepEqual(scheduleBody({ startsAt: '2026-10-11T09:00', endsAt: '2026-10-11T10:00' }), { startsAt: '2026-10-11T09:00:00', endsAt: '2026-10-11T10:00:00' });
  assert.throws(() => scheduleBody({ startsAt: '2026-10-11T10:00', endsAt: '2026-10-11T09:00' }), /结束时间/);
  const body = adBody({ name: '广告', position: 'web_index_lb', imageUrl: 'https://cdn.example/a.png', targetUrl: '/#/search', status: '1', sortOrder: '3', startAt: '2026-10-11T09:00', endAt: '2026-10-11T10:00' }, { version: '9007199254740993' });
  assert.match(body.startAt, /Z$/); assert.equal(body.version, 9007199254740993n);
  assert.equal(body.targetUrl, '/#/search'); assert.equal(body.status, '1');
});
test('upload checks signature, emptiness and exact 10 MiB limit', async () => {
  const png = new Blob([Uint8Array.from([137, 80, 78, 71, 13, 10, 26, 10])]); assert.equal(await validateImage(png), png);
  const jpeg = new Blob([Uint8Array.from([255, 216, 255, 224])]); assert.equal(await validateImage(jpeg), jpeg);
  await assert.rejects(validateImage(new Blob()), /非空/);
  await assert.rejects(validateImage(new Blob(['not an image'], { type: 'image/png' })), /仅支持/);
  const limit = new Blob([png, new Uint8Array(10 * 1024 * 1024 - png.size)]); assert.equal(await validateImage(limit), limit);
  await assert.rejects(validateImage(new Blob([limit, new Uint8Array(1)])), /10 MiB/);
});
test('server content cannot introduce active HTML, links or images', () => {
  assert.equal(escape('<img onerror="alert(1)">'), '&lt;img onerror=&quot;alert(1)&quot;&gt;');
  for (const value of ['javascript:alert(1)', '//evil.example', '/\\evil.example']) assert.equal(safeLink(value), '');
  assert.equal(safeLink('/#/search'), '/#/search'); assert.doesNotMatch(image('javascript:alert(1)'), /src=/);
  assert.doesNotMatch(permissionTree([{ code: '<script>', name: '<img onerror=bad>', children: [] }]), /<script>|<img /);
  const html = searchResults({ facets: [], rows: [{ name: '<img onerror=bad>', nameHighlight: '<script>bad</script>', image: 'javascript:bad', brandName: '', categoryName: '', specs: {}, price: 100, skuId: '12' }], total: 1, pageNo: 1, totalPages: 1 }, {}, {});
  assert.doesNotMatch(html, /<script>|<img /); assert.match(html, /&lt;img/);
});
test('older requests cannot replace the latest page or detached route', async () => {
  const target = { innerHTML: '', isConnected: true }; let release;
  const first = loadInto(target, () => new Promise(resolve => { release = resolve; }));
  await loadInto(target, async () => 'latest'); release('stale'); await first; assert.equal(target.innerHTML, 'latest');
  target.isConnected = false; await loadInto(target, async () => 'detached'); assert.notEqual(target.innerHTML, 'detached');
});
test('legacy context never propagates to product/ad/search endpoints', async () => {
  const calls = [];
  const client = createApi({ mode: 'bearer', legacyTenantId: '2', legacyActorId: '3', fetcher: async (url, options) => {
    calls.push({ url, ...options }); return new Response('{"success":true,"data":true}');
  } });
  for (const path of ['/api/v1/brands', '/api/users', '/api/v1/products', '/api/v1/ads', '/api/v1/product-search', '/api/v1/ad-slots/web_index_lb/ads']) await client.request(path, { method: 'POST', body: {} });
  assert.equal(calls[0].headers['X-Tenant-Id'], '2'); assert.equal(calls[0].headers['X-Actor-Id'], '3'); assert.equal(calls[1].headers['X-Operator-Id'], '3');
  for (const call of calls.slice(2)) assert.equal(call.headers['X-Tenant-Id'], undefined);
});
