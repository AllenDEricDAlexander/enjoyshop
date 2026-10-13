import { api, segment } from '../api.js';
import { integer, version, httpsUrl } from '../contracts.js';
import { pageHeader, fields, field, form, onSubmit, button, escape, table, pager, image, loadInto,
  bindRetry, bindPaging, openDialog, confirmAction, notify, detailList, bindImageUpload, uploadControl, upload } from '../ui.js';

export function renderBrands(root) {
  root.innerHTML = pageHeader('品牌管理', '维护品牌资料、首字母索引和显示顺序。', button('＋ 新建品牌', 'create', '', 'primary')) +
    `<section class="panel">${form('brand-filters', `<div class="filters">${field('name', '品牌名称', '', { maxlength: 120, placeholder: '按名称前缀查询' })}${field('letter', '首字母', '', { maxlength: 1, pattern: '[A-Za-z]' })}${field('categoryName', '分类名称', '', { maxlength: 120 })}${field('seqMin', '排序下限', '', { type: 'number', min: 0 })}${field('seqMax', '排序上限', '', { type: 'number', min: 0 })}</div>`, '查询')}<div id="brand-list"></div></section>`;
  const list = root.querySelector('#brand-list');
  let filters = {}, pageNo = 1, rows = [];
  const refresh = () => loadInto(list, async () => {
    const page = await api('/api/v1/brands', { query: { ...filters, pageNo, pageSize: 20 } });
    rows = page.rows;
    return table(['品牌', '首字母', '排序', '版本', '操作'], rows.map(item => [
      `<div class="item-cell">${image(item.image, item.name)}<span><strong>${escape(item.name)}</strong><small>${escape(item.id)}</small></span></div>`,
      escape(item.letter), escape(item.seq), escape(item.version),
      `<div class="row-actions">${button('详情', 'detail', item.id)}${button('编辑', 'edit', item.id)}${button('删除', 'delete', item.id, 'danger')}</div>`,
    ])) + pager(page);
  });
  bindRetry(list, refresh);
  bindPaging(list, page => { pageNo = page; refresh(); });
  onSubmit(root.querySelector('#brand-filters'), async values => {
    filters = Object.fromEntries(Object.entries(values).map(([key, value]) => [key, value.trim()]));
    if (values.seqMin !== '') filters.seqMin = integer(values.seqMin, '排序下限');
    if (values.seqMax !== '') filters.seqMax = integer(values.seqMax, '排序上限');
    if (values.seqMin !== '' && values.seqMax !== '' && filters.seqMax < filters.seqMin) throw new Error('排序上限不得小于下限');
    pageNo = 1;
    await refresh();
  });
  root.addEventListener('click', async event => {
    const control = event.target.closest('[data-action]');
    if (!control || !['create', 'detail', 'edit', 'delete'].includes(control.dataset.action) || control.disabled) return;
    control.disabled = true;
    try {
      const action = control.dataset.action;
      // 编辑与删除前重新读取，表单持有这次读取的版本。
      const item = action === 'create' ? null : await api(`/api/v1/brands/${segment(control.dataset.id)}`);
      if (!root.isConnected) return;
      if (action === 'detail') openDialog('品牌详情', `${image(item.image, item.name, 'detail-image')}${detailList({ '名称': item.name, '品牌 ID': item.id, '首字母': item.letter, '排序': item.seq, '版本': item.version, '图片': item.image })}`);
      if (action === 'delete') confirmAction('删除品牌', `确认删除“${item.name}”？该操作将由后端检查品牌引用与版本。`, async () => {
        await api(`/api/v1/brands/${segment(item.id)}`, { method: 'DELETE', query: { version: item.version } });
        notify('品牌已删除'); await refresh();
      });
      if (action === 'create' || action === 'edit') editBrand(item, refresh);
    } catch (error) { notify(error.message); }
    finally { control.disabled = false; }
  });
  refresh();
}
function editBrand(item, refresh) {
  const dialog = openDialog(item ? '编辑品牌' : '新建品牌', form('brand-editor', fields(
    field('name', '品牌名称', item?.name, { required: true, maxlength: 120 }) +
    field('letter', '首字母', item?.letter, { required: true, maxlength: 1, pattern: '[A-Za-z]' }) +
    field('seq', '排序权重', item?.seq ?? 0, { required: true, type: 'number', min: 0, max: 999999 }) +
    field('image', '图片 URL', item?.image, { type: 'url', maxlength: 500, hint: '可留空清除图片' })
  ) + uploadControl('image'), '保存品牌'));
  bindImageUpload(dialog);
  onSubmit(dialog.querySelector('form'), async values => {
    const body = { name: values.name.trim(), letter: values.letter.toUpperCase(), seq: integer(values.seq, '排序', 0, 999999), image: values.image.trim() || null };
    if (item) body.version = version(item.version);
    await api(item ? `/api/v1/brands/${segment(item.id)}` : '/api/v1/brands', { method: item ? 'PUT' : 'POST', body });
    dialog.close(); notify('品牌已保存'); await refresh();
  });
}
export function renderSpecs(root) {
  root.innerHTML = pageHeader('分类规格', '按分类名称查询已配置的规格与选项。') +
    `<section class="panel">${form('spec-query', fields(field('categoryName', '分类名称', '', { required: true, maxlength: 120, placeholder: '输入完整分类名称' })), '查询规格')}<div id="spec-result"></div></section>`;
  onSubmit(root.querySelector('form'), async ({ categoryName }) => {
    const rows = await api(`/api/v1/specs/category/${segment(categoryName.trim())}`);
    root.querySelector('#spec-result').innerHTML = table(['规格名称', '可选值'], rows.map(item => [escape(item.name), `<div class="tags">${item.options.map(option => `<span class="tag">${escape(option)}</span>`).join('')}</div>`]));
  });
}
export function renderFiles(root) {
  root.innerHTML = pageHeader('图片上传', '上传 JPEG 或 PNG 图片，取得可用于商品、品牌和广告的公开地址。') +
    `<div class="two-columns"><section class="panel"><h2>选择图片</h2>${form('file-upload', '<label class="file-drop"><span class="empty-symbol">↑</span><strong>选择 JPEG / PNG 图片</strong><small>单张最大 10 MiB</small><input name="file" type="file" accept="image/jpeg,image/png" required></label>', '上传图片')}</section><section class="panel"><h2>上传结果</h2><div id="upload-result"><p class="muted">上传成功后在这里预览并复制地址。</p></div></section></div>`;
  onSubmit(root.querySelector('form'), async ({ file }) => {
    const url = await upload(file);
    httpsUrl(url, false);
    root.querySelector('#upload-result').innerHTML = `${image(url, '上传的图片', 'detail-image')}${field('publicUrl', '公开地址', url, { readonly: true })}${button('复制地址', 'copy')}`;
    notify('图片上传成功');
  });
  root.addEventListener('click', async event => {
    if (!event.target.closest('[data-action="copy"]')) return;
    try { await navigator.clipboard.writeText(root.querySelector('[name="publicUrl"]').value); notify('地址已复制'); }
    catch { notify('请选中公开地址并手动复制'); }
  });
}
