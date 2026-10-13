import { api, configureApi, connection, segment } from '../api.js';
import { pageHeader, form, field, fields, onSubmit, button, detailList, notify, escape, loadInto, bindRetry } from '../ui.js';

export function renderOverview(root) {
  const entries = [
    ['products', '商品管理', '管理 SPU 与 SKU，审核、上下架和回收站', '01', 'blue'],
    ['brands', '品牌与规格', '维护品牌资料，查询分类规格选项', '02', 'violet'],
    ['ads', '广告运营', '配置广告位、投放时间与展示顺序', '03', 'orange'],
    ['search', '商城搜索', '按价格、分类、品牌和规格筛选 SKU', '04', 'green'],
    ['users', '用户与权限', '创建用户、分配角色并查看权限树', '05', 'blue'],
    ['classes', '课程与排课', '创建课程和班级，安排课程时间', '06', 'violet'],
  ];
  root.innerHTML = pageHeader('工作概览', '从商品上架到内容投放，让日常运营井然有序。') +
    `<section class="welcome-panel"><div><span class="eyebrow">YOUR DAILY WORKSPACE</span><h2>每一份好商品，<br>值得被看见。</h2><p>选择一个工作区域，开始今天的管理工作。</p><a class="button primary" href="#/products">进入商品中心 <span aria-hidden="true">→</span></a></div><div class="welcome-art" aria-hidden="true"><div class="art-box">E</div><div class="art-ring"></div><span class="art-label">ENJOY THE LITTLE THINGS</span></div></section>` +
    `<div class="section-heading"><h2>工作区域</h2><a href="#/connection">管理连接 →</a></div><div class="entry-grid">${entries.map(([route, title, description, number, color]) => `<a class="entry-card ${color}" href="#/${route}"><div class="entry-number">${number}</div><h3>${title}<span aria-hidden="true">↗</span></h3><p>${description}</p></a>`).join('')}</div>` +
    `<div class="info-strip"><span>开始前</span><p>通过“连接与登录”接入实际环境。页面数据来自后端；操作权限由服务端校验。</p><a href="#/connection">配置连接 →</a></div>`;
}
export function renderConnection(root) {
  root.innerHTML = pageHeader('连接与登录', '使用平台登录会话，或连接本地开发接口。凭据仅保存在当前页面内存中。') +
    `<div class="two-columns"><section class="panel"><h2>接口连接</h2>${form('connection-form', fields(
      field('base', '接口地址', connection.base, { type: 'url', placeholder: '留空使用当前站点', hint: '同源 Edge/BFF 推荐留空；本地可使用 http://localhost:端口' }) +
      field('mode', '认证方式', connection.mode, { choices: [['bff', '平台登录会话（BFF）'], ['bearer', '开发环境 Bearer']] }) +
      field('registration', '登录配置名称', connection.registration, { placeholder: '实际 OIDC registration ID', hint: '由平台配置提供，用于发起单点登录' }) +
      field('token', '开发环境访问凭据', '', { type: 'password', autocomplete: 'off', hint: '仅 Bearer 模式使用；刷新页面后清除' }) +
      field('legacyTenantId', '开发租户 ID（旧接口）', connection.legacyTenantId, { inputmode: 'numeric', hint: '仅开发 Bearer 模式的品牌、规格、用户和教学接口使用' }) +
      field('legacyActorId', '开发操作人 ID（旧接口）', connection.legacyActorId, { inputmode: 'numeric', hint: '旧接口写入上下文；BFF 模式由网关提供，商品和广告不使用此字段' })
    ), '应用连接')}</section><section class="panel"><h2>当前登录状态</h2><div id="session-result"><p class="muted">读取会话以确认当前登录身份。</p></div><div class="actions">${button('刷新会话', 'session')}${button('平台登录', 'login', '', 'primary')}${button('退出登录', 'logout', '', 'danger')}</div><div class="form-error" role="alert" hidden></div></section></div>`;
  const session = root.querySelector('#session-result');
  const errorTarget = root.querySelector('.panel:last-child');
  const refresh = () => loadInto(session, async current => {
    if (connection.mode !== 'bff') return '<p class="muted">正在使用开发环境凭据。实际权限由每次业务请求校验。</p>';
    const identity = await api('/bff/session');
    if (current()) document.querySelector('#session-label').textContent = identity.displayName || '连接与登录';
    return detailList({ '登录状态': identity.authenticated ? '已登录' : '未登录', '姓名': identity.displayName, '用户': identity.subject, '租户': identity.tenantId });
  });
  bindRetry(session, refresh);
  onSubmit(root.querySelector('#connection-form'), async values => {
    configureApi(values);
    root.querySelector('[name="token"]').value = '';
    document.querySelector('#session-label').textContent = '连接与登录';
    notify('连接已应用，凭据未持久化');
    await refresh();
  });
  errorTarget.addEventListener('click', async event => {
    const control = event.target.closest('[data-action]');
    if (!control || control.disabled) return;
    const errorNode = errorTarget.querySelector('.form-error');
    errorNode.hidden = true;
    control.disabled = true;
    try {
      if (control.dataset.action === 'session') await refresh();
      if (control.dataset.action === 'login') {
        if (connection.mode !== 'bff' || !connection.registration.trim()) throw new Error('请先配置 BFF 模式和真实登录配置名称');
        location.assign(`${connection.base}/oauth2/authorization/${segment(connection.registration)}`);
      }
      if (control.dataset.action === 'logout') {
        if (connection.mode === 'bff') await api('/bff/logout', { method: 'POST' });
        else configureApi({ ...connection, token: '' });
        document.querySelector('#session-label').textContent = '连接与登录';
        notify('已退出登录');
        await refresh();
      }
    } catch (error) {
      if (error.body?.localLoggedOut) {
        document.querySelector('#session-label').textContent = '连接与登录';
        await refresh();
      }
      errorNode.textContent = error.message;
      errorNode.hidden = false;
    } finally { control.disabled = false; }
  });
}
