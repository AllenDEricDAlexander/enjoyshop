import { api } from '../api.js';
import { id, money, integer } from '../contracts.js';
import { pageHeader, field, form, onSubmit, button, escape, image, pager, loadInto, bindRetry, bindPaging, empty, badge, notify } from '../ui.js';

export function searchQuery(values, specs = {}, pageNo = 1) {
  const query = { keywords: values.keywords.trim(), pageNo, pageSize: 20, sort: values.sort };
  for (const name of ['categoryId', 'brandId']) if (values[name]) query[name] = id(values[name], name === 'categoryId' ? '分类 ID' : '品牌 ID');
  for (const name of ['minPrice', 'maxPrice']) if (values[name] !== '') query[name] = money(values[name]);
  if (query.minPrice !== undefined && query.maxPrice !== undefined && query.minPrice > query.maxPrice) throw new Error('最高价格不得小于最低价格');
  for (const [name, options] of Object.entries(specs)) if (options.length) query[`spec.${name}`] = options;
  if (pageNo * query.pageSize > 10000) throw new Error('搜索分页不能超过 10000 条窗口');
  return query;
}
export function renderSearch(root) {
  root.innerHTML = pageHeader('商城搜索', '按名称、分类、品牌、价格和规格，查找当前可售 SKU。') +
    `<section class="panel">${form('search-filters', `<div class="filters">${field('keywords', '关键词', '', { maxlength: 100, placeholder: '你想寻找什么？' })}${field('brandId', '品牌 ID', '', { inputmode: 'numeric' })}${field('categoryId', '分类 ID', '', { inputmode: 'numeric' })}${field('minPrice', '最低价格（元）', '', { type: 'number', min: 0, step: '0.01' })}${field('maxPrice', '最高价格（元）', '', { type: 'number', min: 0, step: '0.01' })}${field('sort', '排序', 'relevance', { choices: [['relevance', '相关度'], ['priceAsc', '价格从低到高'], ['priceDesc', '价格从高到低'], ['salesDesc', '销量优先'], ['createdDesc', '最新商品']] })}</div>`, '搜索商品', button('重置', 'reset'))}<div id="selected-specs"></div></section><div id="search-results"></div>`;
  const results = root.querySelector('#search-results');
  const filterForm = root.querySelector('#search-filters');
  let values = Object.fromEntries(new FormData(filterForm)), specs = {}, pageNo = 1, facets = [];
  function showSelected() {
    root.querySelector('#selected-specs').innerHTML = `<div class="tags">${Object.entries(specs).flatMap(([name, options]) => options.map(value => `<button type="button" class="tag selected" data-remove-spec="${escape(name)}" data-value="${escape(value)}">${escape(name)}：${escape(value)} ×</button>`)).join('')}</div>`;
  }
  const refresh = () => loadInto(results, async current => {
    const page = await api('/api/v1/product-search', { query: searchQuery(values, specs, pageNo) });
    if (current()) facets = page.facets;
    return searchResults(page, specs, values);
  });
  bindRetry(results, refresh);
  bindPaging(results, page => { pageNo = page; refresh(); });
  onSubmit(filterForm, async next => {
    searchQuery(next, specs);
    values = next; pageNo = 1; await refresh();
  });
  root.addEventListener('click', event => {
    const facetControl = event.target.closest('[data-facet]');
    const remove = event.target.closest('[data-remove-spec]');
    const reset = event.target.closest('[data-action="reset"]');
    if (reset) {
      filterForm.reset(); values = Object.fromEntries(new FormData(filterForm)); specs = {}; pageNo = 1;
      showSelected(); refresh();
    }
    if (remove) {
      specs[remove.dataset.removeSpec] = specs[remove.dataset.removeSpec].filter(value => value !== remove.dataset.value);
      pageNo = 1; showSelected(); refresh();
    }
    if (facetControl) {
      const facet = facets[integer(facetControl.dataset.facet, '候选', 0, 21)];
      const key = facetControl.dataset.key;
      if (facet.kind === 'spec') {
        const selected = specs[facet.name] || [];
        const next = selected.includes(key) ? selected.filter(value => value !== key) : [...selected, key];
        if (next.length > 20) { notify('同一规格最多选择 20 个值'); return; }
        Object.defineProperty(specs, facet.name, { value: next, enumerable: true, writable: true, configurable: true });
      } else {
        const fieldName = facet.kind === 'brand' ? 'brandId' : 'categoryId';
        values[fieldName] = values[fieldName] === key ? '' : key;
        filterForm.elements[fieldName].value = values[fieldName];
      }
      pageNo = 1; showSelected(); refresh();
    }
  });
  refresh();
}

export function searchResults(page, specs, values) {
  const facets = page.facets.map((facet, index) => {
    const heading = facet.kind === 'brand' ? '品牌' : facet.kind === 'category' ? '分类' : facet.name;
    const candidates = facet.buckets.map(bucket => {
      const selected = facet.kind === 'spec' ? specs[facet.name]?.includes(bucket.key)
        : values[facet.kind === 'brand' ? 'brandId' : 'categoryId'] === bucket.key;
      return `<button type="button" class="facet-option ${selected ? 'selected' : ''}" data-facet="${index}" data-key="${escape(bucket.key)}"><span>${escape(bucket.label)}</span><small>${escape(bucket.count)}</small></button>`;
    }).join('');
    return `<section><h3>${escape(heading)}</h3>${candidates}${facet.truncated ? '<small class="muted">候选已截断，可继续缩小筛选范围。</small>' : ''}</section>`;
  }).join('');
  const cards = page.rows.map(item => `<article class="product-card">${image(item.image, item.name, 'product-image')}<div class="product-card-body"><small>${escape(item.brandName)} · ${escape(item.categoryName)}</small><h3>${escape(item.name)}</h3><p>${escape(Object.entries(item.specs).map(([key, value]) => `${key} ${value}`).join(' / '))}</p><strong class="price">¥ ${(item.price / 100).toFixed(2)}</strong><small class="sku-id">SKU ${escape(item.skuId)}</small></div></article>`).join('');
  return `<div class="search-layout"><aside class="panel facets"><h2>筛选条件</h2>${facets || '<p class="muted">暂无可用候选。</p>'}${page.facetsTruncated ? '<p class="muted">部分候选未展示。</p>' : ''}</aside><section><div class="section-heading"><h2>搜索结果</h2>${badge(`${page.total} 个 SKU`)}</div>${cards ? `<div class="product-grid">${cards}</div>` : empty('没有找到商品', '试试更少的关键词或更宽的筛选条件。')}${pager(page)}</section></div>`;
}
