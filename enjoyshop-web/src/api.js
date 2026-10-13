// 品牌 ID 仍以 JSON Long 返回；在 JSON.parse 之前保护超出安全范围的整数。
export function parseJson(text) {
  let output = '', index = 0;
  while (index < text.length) {
    if (text[index] === '"') {
      const start = index++;
      while (index < text.length) {
        if (text[index] === '\\') { index += 2; continue; }
        if (text[index++] === '"') break;
      }
      output += text.slice(start, index);
    } else if (text[index] === '-' || /[0-9]/.test(text[index])) {
      const match = text.slice(index).match(/^-?\d+(?:\.\d+)?(?:[eE][+-]?\d+)?/);
      if (!match) throw new Error('响应 JSON 格式无效');
      const value = match[0];
      output += /^-?\d+$/.test(value) && !Number.isSafeInteger(Number(value)) ? `"${value}"` : value;
      index += value.length;
    } else { output += text[index++]; }
  }
  return JSON.parse(output);
}

// BigInt 只用于必须以 JSON 整数回传的版本，ID 保持字符串。
export function stringifyJson(value) {
  if (typeof value === 'bigint') return value.toString();
  if (Array.isArray(value)) return `[${value.map(stringifyJson).join(',')}]`;
  if (value && typeof value === 'object') {
    return `{${Object.entries(value).filter(([, item]) => item !== undefined)
      .map(([key, item]) => `${JSON.stringify(key)}:${stringifyJson(item)}`).join(',')}}`;
  }
  return JSON.stringify(value);
}

export function queryString(query = {}) {
  const parameters = new URLSearchParams();
  for (const [key, value] of Object.entries(query)) {
    if (value === '' || value === null || value === undefined) continue;
    for (const item of Array.isArray(value) ? value : [value]) parameters.append(key, String(item));
  }
  return parameters.size ? `?${parameters}` : '';
}

export class ApiError extends Error {
  constructor(message, status, body) {
    super(message);
    this.status = status;
    this.body = body;
  }
}

export function validateBase(value) {
  if (!value) return '';
  const url = new URL(value);
  if (!['https:', 'http:'].includes(url.protocol) || url.username || url.password || url.search || url.hash) {
    throw new Error('接口地址须为无账号、查询参数和片段的 HTTP(S) 地址');
  }
  if (url.protocol === 'http:' && !['localhost', '127.0.0.1', '[::1]'].includes(url.hostname)) {
    throw new Error('远端接口须使用 HTTPS');
  }
  return url.href.replace(/\/$/, '');
}

export function createApi({ base = '', mode = 'bff', token = '', fetcher = globalThis.fetch, locationHash = globalThis.location } = {}) {
  base = validateBase(base);
  if (!['bff', 'bearer'].includes(mode)) throw new Error('未知连接方式');
  let bearer = token;
  async function request(path, { method = 'GET', query, body } = {}) {
    const headers = { Accept: 'application/json' };
    const write = method !== 'GET';
    if (mode === 'bearer' && bearer) headers.Authorization = `Bearer ${bearer}`;
    if (mode === 'bff' && write) {
      const csrf = await request('/bff/csrf');
      if (!csrf?.headerName || !csrf?.token) throw new ApiError('无法获取 CSRF 凭据，请重新登录', 403, csrf);
      headers[csrf.headerName] = csrf.token;
    }
    const multipart = typeof FormData !== 'undefined' && body instanceof FormData;
    if (body !== undefined && !multipart) headers['Content-Type'] = 'application/json';
    let response, text;
    try {
      response = await fetcher(`${base}${path}${queryString(query)}`, {
        method, headers, credentials: mode === 'bff' ? 'include' : 'omit',
        redirect: 'error', cache: 'no-store', signal: AbortSignal.timeout(30000),
        ...(body === undefined ? {} : { body: multipart ? body : stringifyJson(body) }),
      });
      text = await response.text();
      if (response.status === 401 && mode === 'bff' && locationHash && locationHash.hash !== '#/connection') {
        locationHash.hash = '#/connection';
      }
    } catch {
      throw new ApiError(write
        ? '网络中断或请求超时，操作结果可能未知。请先查询确认，再决定是否重新提交。'
        : '无法连接接口，请检查网络、接口地址或网关配置', 0);
    }
    let result;
    try { result = text ? parseJson(text) : null; }
    catch { throw new ApiError('接口返回了非 JSON 内容，请检查网关路由或登录状态', response.status); }
    if (!response.ok || result?.success === false) {
      const fallback = { 401: '请先登录或更新凭据', 403: '当前身份没有此操作权限',
        404: '资源不存在或功能尚未配置', 409: '数据版本或状态已变化，请刷新后重新操作',
        413: '文件过大，请选择 10 MiB 以内的图片', 503: '服务或依赖当前不可用' };
      throw new ApiError(result?.message || result?.error || fallback[response.status] || '操作失败', response.status, result);
    }
    if (result && Object.hasOwn(result, 'records')) return { rows: result.records, ...result.page };
    return result && Object.hasOwn(result, 'success') ? result.data : result;
  }
  return { request, clearToken() { bearer = ''; } };
}

const config = globalThis.window?.ENJOYSHOP_CONFIG || {};
export const connection = { base: config.apiBase || '', mode: config.authMode || 'bff', registration: config.loginRegistrationId || '', legacyTenantId: '', legacyActorId: '' };
let activeApi = createApi({ base: connection.base, mode: connection.mode });
export function configureApi({ base, mode, token, registration, legacyTenantId = '', legacyActorId = '' }) {
  for (const value of [legacyTenantId, legacyActorId]) {
    if (value && (!/^[1-9]\d{0,18}$/.test(value) || BigInt(value) > 9223372036854775807n)) throw new Error('开发租户和操作人 ID 须为有效正整数');
  }
  const next = createApi({ base, mode, token });
  activeApi.clearToken();
  Object.assign(connection, { base: validateBase(base), mode, registration, legacyTenantId, legacyActorId });
  activeApi = next;
}
export const api = (path, options) => activeApi.request(path, options);
export const segment = value => encodeURIComponent(String(value));
