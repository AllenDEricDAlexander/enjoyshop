import { api, segment } from '../api.js';
import { integer, version, httpsUrl } from '../contracts.js';
import { pageHeader, fields, field, form, onSubmit, button, escape, table, pager, image, loadInto,
  bindRetry, bindPaging, openDialog, confirmAction, notify, detailList, bindImageUpload, uploadControl, safeLink, empty } from '../ui.js';

const position = () => window.ENJOYSHOP_CONFIG?.adPosition || 'web_index_lb';
export function localInstant(value) {
  if (!value) return '';
  const date = new Date(value);
  const pad = number => String(number).padStart(2, '0');
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}.${String(date.getMilliseconds()).padStart(3, '0')}`;
}
export function adBody(values, item) {
  const start = new Date(values.startAt), end = new Date(values.endAt);
  if (!Number.isFinite(start.getTime()) || !Number.isFinite(end.getTime()) || end <= start) throw new Error('投放结束时间必须晚于开始时间');
  const target = values.targetUrl.trim();
  if (!safeLink(target)) throw new Error('跳转地址须为 HTTPS 地址或以单个 / 开始的站内路径');
  return { name: values.name.trim(), position: values.position.trim(), imageUrl: httpsUrl(values.imageUrl.trim(), false), targetUrl: target,
    status: values.status, sortOrder: integer(values.sortOrder, '排序', 0, 1000000), startAt: start.toISOString(), endAt: end.toISOString(),
    ...(item ? { version: version(item.version) } : {}) };
}
export function renderAds(root) {
  root.innerHTML = pageHeader('广告管理', '管理投放内容、时间窗口和展示顺序。', button('＋ 新建广告', 'create', '', 'primary')) +
    `<section class="panel">${form('ad-filters', `<div class="filters">${field('position', '广告位', '', { maxlength: 64, placeholder: position() })}${field('status', '状态', '', { choices: [['', '全部'], ['1', '启用'], ['0', '禁用']] })}</div>`, '查询')}<div id="ad-list"></div></section>`;
  const list = root.querySelector('#ad-list');
  let filters = {}, pageNo = 1;
  const refresh = () => loadInto(list, async () => {
    const page = await api('/api/v1/ads', { query: { ...filters, pageNo, pageSize: 20 } });
    return table(['广告', '广告位', '状态', '投放时间', '排序', '操作'], page.rows.map(item => [
      `<div class="item-cell">${image(item.imageUrl, item.name)}<span><strong>${escape(item.name)}</strong><small>${escape(item.id)}</small></span></div>`,
      escape(item.position), `<span class="badge ${item.status === '1' ? 'green' : ''}">${item.status === '1' ? '启用' : '禁用'}</span>`,
      `<div class="time-cell">${escape(new Date(item.startAt).toLocaleString())}<small>至 ${escape(new Date(item.endAt).toLocaleString())}</small></div>`, escape(item.sortOrder),
      `<div class="row-actions">${button('详情', 'detail', item.id)}${button('编辑', 'edit', item.id)}${button('删除', 'delete', item.id, 'danger')}</div>`,
    ])) + pager(page);
  });
  bindRetry(list, refresh);
  bindPaging(list, page => { pageNo = page; refresh(); });
  onSubmit(root.querySelector('#ad-filters'), async values => { filters = values; pageNo = 1; await refresh(); });
  root.addEventListener('click', async event => {
    const control = event.target.closest('[data-action]');
    if (!control || !['create', 'detail', 'edit', 'delete'].includes(control.dataset.action) || control.disabled) return;
    control.disabled = true;
    try {
      const action = control.dataset.action;
      const item = action === 'create' ? null : await api(`/api/v1/ads/${segment(control.dataset.id)}`);
      if (!root.isConnected) return;
      if (action === 'detail') openDialog('广告详情', `${image(item.imageUrl, item.name, 'detail-image')}${detailList({ '名称': item.name, '广告 ID': item.id, '广告位': item.position, '跳转地址': item.targetUrl, '状态': item.status === '1' ? '启用' : '禁用', '开始': new Date(item.startAt).toLocaleString(), '结束': new Date(item.endAt).toLocaleString(), '排序': item.sortOrder, '版本': item.version })}`);
      if (action === 'delete') confirmAction('删除广告', `确认删除“${item.name}”？`, async () => {
        await api(`/api/v1/ads/${segment(item.id)}`, { method: 'DELETE', query: { version: item.version } });
        notify('广告已删除'); await refresh();
      });
      if (action === 'create' || action === 'edit') editAd(item, refresh);
    } catch (error) { notify(error.message); }
    finally { control.disabled = false; }
  });
  refresh();
}
function editAd(item, refresh) {
  const dialog = openDialog(item ? '编辑广告' : '新建广告', form('ad-editor', fields(
    field('name', '广告名称', item?.name, { required: true, maxlength: 120 }) +
    field('position', '广告位', item?.position || position(), { required: true, maxlength: 64, hint: '须为后端已配置的广告位' }) +
    field('imageUrl', '图片地址', item?.imageUrl, { required: true, type: 'url', maxlength: 512, hint: '须位于后端配置的公开图片目录' }) +
    field('targetUrl', '跳转地址', item?.targetUrl, { required: true, maxlength: 1024, placeholder: '/#/search 或 https://…' }) +
    field('status', '状态', item?.status || '0', { choices: [['0', '禁用'], ['1', '启用']] }) +
    field('sortOrder', '排序', item?.sortOrder ?? 0, { required: true, type: 'number', min: 0, max: 1000000 }) +
    field('startAt', '投放开始', localInstant(item?.startAt), { required: true, type: 'datetime-local', step: '0.001', hint: '按当地时间输入，保存时转换为 UTC' }) +
    field('endAt', '投放结束', localInstant(item?.endAt), { required: true, type: 'datetime-local', step: '0.001' })
  ) + uploadControl('imageUrl'), '保存广告'));
  bindImageUpload(dialog);
  onSubmit(dialog.querySelector('form'), async values => {
    await api(item ? `/api/v1/ads/${segment(item.id)}` : '/api/v1/ads', { method: item ? 'PUT' : 'POST', body: adBody(values, item) });
    dialog.close(); notify('广告已保存'); await refresh();
  });
}
export function renderShowcase(root) {
  root.innerHTML = pageHeader('广告展示', '查看站点当前可展示的广告内容。') +
    `<section class="panel">${form('ad-slot', fields(field('position', '广告位', position(), { required: true, maxlength: 64 }) + field('limit', '最多展示条数', 10, { required: true, type: 'number', min: 1, max: 20 })), '加载广告')}<div id="ad-showcase"></div></section>`;
  const result = root.querySelector('#ad-showcase');
  onSubmit(root.querySelector('form'), async values => {
    const items = await api(`/api/v1/ad-slots/${segment(values.position.trim())}/ads`, { query: { limit: integer(values.limit, '条数', 1, 20) } });
    result.innerHTML = items.length ? `<div class="ad-grid">${items.map(item => `<article class="ad-card">${image(item.imageUrl, item.name, 'ad-image')}<h3>${escape(item.name)}</h3>${safeLink(item.targetUrl) ? `<a href="${safeLink(item.targetUrl)}" target="_blank" rel="noopener noreferrer">查看广告 →</a>` : '<span>跳转地址不可用</span>'}</article>`).join('')}</div>` : empty('暂无投放内容', '当前广告位没有处于有效投放时间内的广告。');
  });
}
