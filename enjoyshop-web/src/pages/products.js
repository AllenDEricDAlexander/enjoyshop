import { api, segment } from '../api.js';
import { integer, id, version, httpsUrl, lines, editableProduct, newSku, validateProduct, productActions } from '../contracts.js';
import { pageHeader, fields, field, form, onSubmit, button, escape, table, pager, image, loadInto,
  bindRetry, bindPaging, openDialog, confirmAction, notify, detailList, bindImageUpload, uploadControl, badge, showError } from '../ui.js';

const labels = { detail: '详情', edit: '编辑', approval: '审核并上架', withdrawal: '下架', publication: '上架',
  restoration: '还原', delete: '移入回收站', purge: '永久删除' };
const productState = item => item.isDelete ? badge('已回收') : item.isMarketable ? badge('已上架', 'green') : badge(item.status === '1' ? '已审核 · 下架' : '待审核', 'orange');
export function renderProducts(root, deleted = false) {
  root.innerHTML = pageHeader(deleted ? '商品回收站' : '商品管理', deleted ? '还原已回收商品，或永久清理商品与其历史 SKU。' : '管理商品资料、SKU 库存与价格，完成审核和上下架。', deleted ? '' : button('＋ 新建商品', 'create', '', 'primary')) +
    `<section class="panel">${form('product-filters', `<div class="filters">${field('name', '商品名称', '', { maxlength: 200, placeholder: '查找商品' })}${field('sn', '商品货号', '', { maxlength: 64 })}${deleted ? '' : field('status', '审核状态', '', { choices: [['', '全部'], ['0', '待审核'], ['1', '已审核']] }) + field('isMarketable', '销售状态', '', { choices: [['', '全部'], ['true', '已上架'], ['false', '已下架']] })}</div>`, '查询')}<div id="product-list"></div></section>`;
  const list = root.querySelector('#product-list');
  let filters = {}, pageNo = 1, rows = [];
  const refresh = () => loadInto(list, async current => {
    const page = await api('/api/v1/products', { query: { ...filters, deleted, pageNo, pageSize: 20 } });
    if (current()) rows = page.rows;
    return table(['商品', '货号', '状态', '版本', '操作'], page.rows.map(item => [
      `<div class="item-cell">${image(item.image, item.name)}<span><strong>${escape(item.name)}</strong><small>${escape(item.id)}</small></span></div>`,
      escape(item.sn), productState(item), escape(item.version), `<div class="row-actions">${productActions(item, deleted).map(action => button(labels[action], action, item.id, ['delete', 'purge'].includes(action) ? 'danger' : '')).join('')}</div>`,
    ])) + pager(page);
  });
  bindRetry(list, refresh);
  bindPaging(list, page => { pageNo = page; refresh(); });
  onSubmit(root.querySelector('#product-filters'), async values => { filters = values; pageNo = 1; await refresh(); });
  root.addEventListener('click', async event => {
    const control = event.target.closest('[data-action]');
    if (!control || !['create', ...Object.keys(labels)].includes(control.dataset.action) || control.disabled) return;
    control.disabled = true;
    try {
      const action = control.dataset.action;
      if (action === 'create') { editProduct(null, refresh); return; }
      const row = rows.find(item => item.id === control.dataset.id);
      if (action === 'detail' || action === 'edit') {
        const result = await api(`/api/v1/products/${segment(control.dataset.id)}`, { query: { deleted } });
        if (!root.isConnected) return;
        if (action === 'detail') showProduct(result);
        else if (result.spu.isMarketable || result.spu.isDelete) throw new Error('商品状态已变化，仅可编辑有效且下架的商品');
        else editProduct(result, refresh);
      } else if (row) {
        const description = action === 'purge' ? `永久删除“${row.name}”及全部历史 SKU？此操作不可恢复。`
          : action === 'approval' ? `审核通过“${row.name}”并立即上架？`
          : action === 'restoration' ? `还原“${row.name}”？还原后需要重新审核。` : `确认${labels[action]}“${row.name}”？`;
        confirmAction(labels[action], description, async () => {
          const path = `/api/v1/products/${segment(row.id)}`;
          if (action === 'delete' || action === 'purge') await api(`${path}${action === 'purge' ? '/purge' : ''}`, { method: 'DELETE', query: { version: row.version } });
          else await api(`${path}/${action}`, { method: 'PUT', body: { version: version(row.version) } });
          notify(`${labels[action]}成功`); await refresh();
        });
      }
    } catch (error) { notify(error.message); }
    finally { control.disabled = false; }
  });
  refresh();
}
function showProduct(result) {
  const { spu, skuList } = result;
  openDialog('商品详情', `<div class="product-detail-heading">${image(spu.image, spu.name, 'detail-image')}<div><h3>${escape(spu.name)}</h3><p>${escape(spu.caption)}</p>${productState(spu)}</div></div>` +
    detailList({ '商品 ID': spu.id, '货号': spu.sn, '品牌 ID': spu.brandId, '一级分类': spu.category1Id, '二级分类': spu.category2Id, '三级分类': spu.category3Id, '模板 ID': spu.templateId, '运费模板 ID': spu.freightId, '版本': spu.version, '销量': spu.saleNum, '评论数': spu.commentNum, '创建时间': spu.createTime, '更新时间': spu.updateTime, '删除时间': spu.deletedAt }) +
    `<h3>SKU 明细</h3>${table(['SKU ID', '条码', '规格', '价格（元）', '库存', '预警', '重量（克）', '状态'], skuList.map(sku => [escape(sku.id), escape(sku.sn), escape(Object.entries(sku.spec).map(([key, value]) => `${key}：${value}`).join(' / ')), (sku.price / 100).toFixed(2), escape(sku.num), escape(sku.alertNum), escape(sku.weight), sku.status === '1' ? '启用' : '停用']))}` +
    `<h3>规格与参数</h3>${detailList(Object.fromEntries(Object.entries(spu.specItems).map(([name, options]) => [name, options.join(' / ')])))}${detailList(spu.paraItems)}<h3>售后服务</h3><p>${escape(spu.saleService.join(' / ') || '—')}</p><h3>商品介绍</h3><p class="introduction">${escape(spu.introduction)}</p><div class="image-gallery">${spu.images.map(url => image(url, spu.name, 'gallery-image')).join('')}</div>`);
}
const collectionRow = (index, name = '', value = '', kind = 'spec') => `<div class="collection-row" data-${kind}-row>${field(`${kind}Name${index}`, kind === 'spec' ? '规格名称' : '参数名称', name, { maxlength: 120 })}${field(`${kind}Value${index}`, kind === 'spec' ? '可选值（每行一个）' : '参数值', value, { type: 'textarea', maxlength: kind === 'spec' ? 12100 : 1000 })}${button('移除', `remove-${kind}`)}</div>`;
function collectPairs(root, kind) {
  const result = {};
  for (const row of root.querySelectorAll(`[data-${kind}-row]`)) {
    const name = row.querySelector('input').value.trim();
    const value = row.querySelector('textarea').value;
    if (!name && !value.trim()) continue;
    if (!name || Object.hasOwn(result, name)) throw new Error('规格或参数名称不能为空或重复');
    Object.defineProperty(result, name, { value: kind === 'spec' ? lines(value) : value.trim(), enumerable: true });
  }
  return result;
}
function skuForm(sku, index, specItems) {
  const prefix = `sku${index}_`;
  return `<section class="sku-card" data-sku="${index}"><div class="section-heading"><h3>SKU ${index + 1} <small>${escape(sku.id ? `ID ${sku.id}` : '新增')}</small></h3>${button('移除 SKU', 'remove-sku', index, 'danger')}</div>${fields(
    field(`${prefix}sn`, '条码', sku.sn, { required: true, maxlength: 64 }) +
    field(`${prefix}price`, '价格（分）', sku.price, { required: true, type: 'number', min: 1, max: 2147483647 }) +
    field(`${prefix}num`, '库存', sku.num, { required: true, type: 'number', min: 0, max: 2147483647 }) +
    field(`${prefix}alertNum`, '库存预警值', sku.alertNum, { required: true, type: 'number', min: 0, max: 2147483647 }) +
    field(`${prefix}weight`, '重量（克）', sku.weight, { required: true, type: 'number', min: 0, max: 2147483647 }) +
    field(`${prefix}status`, '状态', sku.status, { choices: [['1', '启用'], ['2', '停用']] }) +
    field(`${prefix}image`, '主图地址', sku.image, { type: 'url', maxlength: 500 }) +
    field(`${prefix}images`, '图片集（每行一个 HTTPS 地址）', sku.images.join('\n'), { type: 'textarea' }) +
    Object.entries(specItems).map(([name, options], optionIndex) => field(`${prefix}spec${optionIndex}`, name, sku.spec[name] ?? '', { choices: [['', '请选择'], ...options.map(value => [value, value])], 'data-spec-name': name })).join('')
  )}${uploadControl(`${prefix}image`)}</section>`;
}
function editProduct(result, refresh) {
  const editable = result ? editableProduct(result) : { spu: { sn: '', name: '', caption: '', brandId: '', category1Id: '', category2Id: '', category3Id: '', templateId: '', freightId: '0', image: '', images: [], saleService: [], introduction: '', isEnableSpec: false, specItems: {}, paraItems: {} }, skuList: [newSku()] };
  const { spu } = editable;
  let skus = editable.skuList;
  let rowSequence = 0;
  const dialog = openDialog(result ? '编辑商品' : '新建商品', form('product-editor',
    '<p class="form-note">编辑仅限下架商品，保存后重新审核。分类与模板 ID 由现有目录提供；SKU 价格单位为分。</p>' +
    `<h3>基本资料</h3>${fields(
      field('sn', '商品货号', spu.sn, { required: true, maxlength: 64 }) + field('name', '商品名称', spu.name, { required: true, maxlength: 200 }) +
      field('caption', '副标题', spu.caption, { maxlength: 300, wide: true }) + field('brandId', '品牌 ID', spu.brandId, { required: true, inputmode: 'numeric' }) +
      field('category1Id', '一级分类 ID', spu.category1Id, { required: true, inputmode: 'numeric' }) + field('category2Id', '二级分类 ID', spu.category2Id, { required: true, inputmode: 'numeric' }) +
      field('category3Id', '三级分类 ID', spu.category3Id, { required: true, inputmode: 'numeric' }) + field('templateId', '规格模板 ID', spu.templateId, { required: true, inputmode: 'numeric' }) +
      field('freightId', '运费模板 ID', spu.freightId, { required: true, inputmode: 'numeric', hint: '仅保存关联值，0 表示未配置' }) +
      field('image', '主图地址', spu.image, { type: 'url', maxlength: 500 }) + field('images', '图片集（每行一个 HTTPS 地址，最多 10 张）', spu.images.join('\n'), { type: 'textarea' }) +
      field('saleService', '售后服务（每行一项，最多 20 项）', spu.saleService.join('\n'), { type: 'textarea' }) + field('introduction', '商品介绍', spu.introduction, { type: 'textarea', maxlength: 20000, wide: true })
    )}${uploadControl('image')}` +
    `<h3>商品规格</h3>${field('isEnableSpec', '启用规格', String(spu.isEnableSpec), { choices: [['false', '不启用（单 SKU）'], ['true', '启用（多规格）']] })}<div id="spec-rows">${Object.entries(spu.specItems).map(([name, values]) => collectionRow(rowSequence++, name, values.join('\n'))).join('')}</div><div class="actions">${button('＋ 添加规格', 'add-spec')}${button('应用选项到 SKU', 'apply-specs')}</div>` +
    `<h3>商品参数</h3><div id="para-rows">${Object.entries(spu.paraItems).map(([name, value]) => collectionRow(rowSequence++, name, value, 'para')).join('')}</div>${button('＋ 添加参数', 'add-para')}` +
    `<div class="section-heading"><h3>SKU 明细</h3>${button('＋ 添加 SKU', 'add-sku')}</div><div id="sku-rows"></div>`, '保存商品'));
  const editor = dialog.querySelector('form');
  const skuRoot = dialog.querySelector('#sku-rows');
  const readSkus = () => Array.from(skuRoot.querySelectorAll('[data-sku]')).map((row, index) => {
    const value = name => row.querySelector(`[name="sku${index}_${name}"]`).value;
    const spec = Object.fromEntries(Array.from(row.querySelectorAll('[data-spec-name]')).map(input => [input.dataset.specName, input.value]));
    return { ...(skus[index].id ? { id: skus[index].id } : {}), sn: value('sn').trim(), price: value('price'), num: value('num'), alertNum: value('alertNum'), weight: value('weight'), status: value('status'), image: value('image').trim(), images: lines(value('images')), spec };
  });
  const renderSkus = () => {
    const specItems = editor.querySelector('[name="isEnableSpec"]').value === 'true' ? collectPairs(editor, 'spec') : {};
    skuRoot.innerHTML = skus.map((sku, index) => skuForm(sku, index, specItems)).join('');
  };
  renderSkus();
  bindImageUpload(dialog);
  dialog.addEventListener('click', event => {
    const control = event.target.closest('[data-action]');
    if (!control || editor.dataset.busy === 'true') return;
    const action = control.dataset.action;
    try {
      if (action === 'add-spec' || action === 'add-para') {
        const kind = action === 'add-spec' ? 'spec' : 'para';
        dialog.querySelector(`#${kind}-rows`).insertAdjacentHTML('beforeend', collectionRow(rowSequence++, '', '', kind));
      }
      if (action === 'remove-spec' || action === 'remove-para') control.closest('.collection-row').remove();
      if (['apply-specs', 'add-sku', 'remove-sku'].includes(action)) {
        skus = readSkus();
        if (action === 'add-sku') {
          if (skus.length >= 100) throw new Error('最多添加 100 个 SKU');
          skus.push(newSku());
        }
        if (action === 'remove-sku') {
          if (skus.length <= 1) throw new Error('商品须至少保留一个 SKU');
          skus.splice(Number(control.dataset.id), 1);
        }
        renderSkus();
      }
    } catch (error) { showError(editor, error); }
  });
  onSubmit(editor, async values => {
    const enable = values.isEnableSpec === 'true';
    const payload = { spu: {
      sn: values.sn.trim(), name: values.name.trim(), caption: values.caption.trim(),
      brandId: id(values.brandId, '品牌 ID'), category1Id: id(values.category1Id, '一级分类 ID'), category2Id: id(values.category2Id, '二级分类 ID'), category3Id: id(values.category3Id, '三级分类 ID'), templateId: id(values.templateId, '模板 ID'), freightId: id(values.freightId, '运费模板 ID', true),
      image: httpsUrl(values.image.trim()), images: lines(values.images).map(url => httpsUrl(url, false)), saleService: lines(values.saleService), introduction: values.introduction,
      isEnableSpec: enable, specItems: enable ? collectPairs(editor, 'spec') : {}, paraItems: collectPairs(editor, 'para'),
    }, skuList: readSkus().map((sku, index) => ({ ...sku, price: integer(sku.price, `SKU ${index + 1} 价格`, 1), num: integer(sku.num, '库存'), alertNum: integer(sku.alertNum, '库存预警'), weight: integer(sku.weight, '重量'), image: httpsUrl(sku.image), images: sku.images.map(url => httpsUrl(url, false)), spec: enable ? sku.spec : {} })), ...(result ? { version: version(editable.version) } : {}) };
    if (payload.spu.images.length > 10 || payload.spu.saleService.length > 20 || payload.skuList.some(sku => sku.images.length > 10)) throw new Error('图片集最多 10 张，售后服务最多 20 项');
    validateProduct(payload);
    await api(result ? `/api/v1/products/${segment(result.spu.id)}` : '/api/v1/products', { method: result ? 'PUT' : 'POST', body: payload });
    dialog.close(); notify('商品已保存，请按流程审核上架'); await refresh();
  });
}
