import { api } from './api.js';
import { integer, httpsUrl, validateImage } from './contracts.js';

export const escape = value => String(value ?? '').replace(/[&<>"']/g, char => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[char]));
export const badge = (text, tone = '') => `<span class="badge ${tone}">${escape(text)}</span>`;
export const button = (text, action, data = '', tone = '') => `<button type="button" class="${tone}" data-action="${escape(action)}" data-id="${escape(data)}">${escape(text)}</button>`;
export function pageHeader(title, description, actions = '') {
  return `<div class="page-heading"><div><div class="eyebrow">ENJOYSHOP WORKSPACE</div><h1>${escape(title)}</h1><p>${escape(description)}</p></div><div class="actions">${actions}</div></div>`;
}
export function field(name, label, value = '', options = {}) {
  const { type = 'text', required = false, placeholder = '', choices, hint = '', wide = false, ...attrs } = options;
  const attributes = Object.entries(attrs).map(([key, item]) => `${key}="${escape(item)}"`).join(' ');
  const common = `name="${escape(name)}" ${required ? 'required' : ''} ${attributes}`;
  let input;
  if (choices) input = `<select ${common}>${choices.map(([key, text]) => `<option value="${escape(key)}" ${String(value) === String(key) ? 'selected' : ''}>${escape(text)}</option>`).join('')}</select>`;
  else if (type === 'textarea') input = `<textarea ${common} placeholder="${escape(placeholder)}">${escape(value)}</textarea>`;
  else input = `<input ${common} type="${escape(type)}" value="${escape(value)}" placeholder="${escape(placeholder)}">`;
  return `<label class="field ${wide ? 'wide' : ''}"><span>${escape(label)}${required ? ' <i>*</i>' : ''}</span>${input}${hint ? `<small>${escape(hint)}</small>` : ''}</label>`;
}
export const fields = content => `<div class="form-grid">${content}</div>`;
export function form(id, content, submit = '保存', extra = '') {
  return `<form id="${id}"><fieldset>${content}<div class="form-error" role="alert" hidden></div><div class="form-actions">${extra}<button type="submit" class="primary">${escape(submit)}</button></div></fieldset></form>`;
}
export function data(formElement) { return Object.fromEntries(new FormData(formElement)); }
export function showError(target, error) {
  const node = target.querySelector('.form-error') || target;
  node.hidden = false;
  node.textContent = errorText(error);
}
export function errorText(error) {
  const detail = error.body?.data?.fieldErrors?.map(item => `${item.field}：${item.message}`).join('；');
  const context = [error.body?.code, error.body?.traceId && `追踪 ${error.body.traceId}`].filter(Boolean).join(' · ');
  return `${error.message}${detail ? `；${detail}` : ''}${context ? `（${context}）` : ''}`;
}
let toastTimer;
export function notify(message) {
  const node = document.querySelector('#notifications');
  node.textContent = message;
  node.classList.add('visible');
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => node.classList.remove('visible'), 4500);
}
export function onSubmit(target, callback) {
  target.addEventListener('submit', async event => {
    event.preventDefault();
    if (target.dataset.busy === 'true' || target.dataset.uploadBusy === 'true') return;
    target.querySelector('.form-error').hidden = true;
    // FormData 不会包含 disabled 控件，必须先读取再禁用。
    const values = data(target);
    target.dataset.busy = 'true';
    const fieldset = target.querySelector('fieldset');
    fieldset.disabled = true;
    try { await callback(values, target); }
    catch (error) { showError(target, error); }
    finally { fieldset.disabled = false; target.dataset.busy = 'false'; }
  });
}
export function openDialog(title, content) {
  const dialog = document.querySelector('#editor');
  if (dialog.open) dialog.close();
  dialog.innerHTML = `<div class="dialog-heading"><h2 id="editor-title">${escape(title)}</h2>${button('关闭', 'close-dialog')}</div>${content}`;
  dialog.querySelector('[data-action="close-dialog"]').onclick = () => { if (dialog.querySelector('[data-busy="true"], [data-upload-busy="true"]')) return; dialog.close(); };
  dialog.oncancel = event => { if (dialog.querySelector('[data-busy="true"], [data-upload-busy="true"]')) event.preventDefault(); };
  dialog.showModal();
  return dialog;
}
export function confirmAction(title, description, callback) {
  const dialog = openDialog(title, form('confirm-form', `<p>${escape(description)}</p>`, '确认操作', button('取消', 'cancel')));
  dialog.querySelector('[data-action="cancel"]').onclick = () => dialog.close();
  onSubmit(dialog.querySelector('form'), async () => { await callback(); dialog.close(); });
}
export function image(url, alt = '', className = 'thumbnail') {
  try { httpsUrl(url, false); }
  catch { return `<span class="image-placeholder ${className}" aria-label="暂无图片">◇</span>`; }
  return `<img class="${className}" src="${escape(url)}" alt="${escape(alt)}" loading="lazy" referrerpolicy="no-referrer">`;
}
export function safeLink(url) {
  try {
    if (/^\/(?!\/)/.test(url) && !/[\\\s]/.test(url)) return escape(url);
    return escape(httpsUrl(url, false));
  } catch { return ''; }
}
export function table(headers, rows) {
  if (!rows.length) return empty('暂无数据', '调整筛选条件，或创建第一条记录。');
  return `<div class="table-scroll"><table><thead><tr>${headers.map(header => `<th scope="col">${escape(header)}</th>`).join('')}</tr></thead><tbody>${rows.map(row => `<tr>${row.map(cell => `<td>${cell}</td>`).join('')}</tr>`).join('')}</tbody></table></div>`;
}
export const empty = (title, description) => `<div class="empty-state"><div class="empty-symbol">◇</div><h3>${escape(title)}</h3><p>${escape(description)}</p></div>`;
export const loading = () => '<div class="loading" role="status">正在加载…</div>';
export function pager(page) {
  const limit = Math.min(Number(page.pages ?? page.totalPages ?? 0), page.maxAccessiblePageNo ?? Infinity);
  const previous = page.pageNo > 1, next = page.pageNo < limit;
  return `<div class="pagination"><span>共 ${escape(page.total ?? 0)} 条 · 第 ${escape(page.pageNo)} 页</span><div><button type="button" data-page="${page.pageNo - 1}" ${previous ? '' : 'disabled'}>上一页</button><button type="button" data-page="${page.pageNo + 1}" ${next ? '' : 'disabled'}>下一页</button></div></div>`;
}
export async function loadInto(target, loader) {
  target.innerHTML = loading();
  const current = Symbol();
  target.loadId = current;
  try {
    const html = await loader(() => target.isConnected && target.loadId === current);
    if (target.isConnected && target.loadId === current) target.innerHTML = html;
  } catch (error) {
    if (target.isConnected && target.loadId === current) target.innerHTML = `<div class="request-error" role="alert"><h3>暂时无法加载</h3><p>${escape(errorText(error))}</p>${button('重新加载', 'retry')}</div>`;
  }
}
export function bindRetry(target, callback) {
  target.addEventListener('click', event => { if (event.target.closest('[data-action="retry"]')) callback(); });
}
export function bindPaging(target, callback) {
  target.addEventListener('click', event => {
    const control = event.target.closest('[data-page]');
    if (control && !control.disabled) callback(integer(control.dataset.page, '页码', 1, 1000000));
  });
}
export function detailList(values) {
  return `<dl class="detail-list">${Object.entries(values).map(([label, value]) => `<div><dt>${escape(label)}</dt><dd>${escape(value ?? '—')}</dd></div>`).join('')}</dl>`;
}
export async function upload(file) {
  await validateImage(file);
  const body = new FormData();
  body.append('file', file);
  return api('/api/v1/files', { method: 'POST', body });
}
export function bindImageUpload(root) {
  root.addEventListener('change', async event => {
    const input = event.target.closest('[data-upload]');
    if (!input || !input.files[0]) return;
    const formElement = input.closest('form');
    if (formElement.dataset.uploadBusy === 'true' || formElement.dataset.busy === 'true') return;
    formElement.dataset.uploadBusy = 'true';
    const fieldset = formElement.querySelector('fieldset');
    fieldset.disabled = true;
    const target = root.querySelector(`[name="${input.dataset.upload}"]`);
    input.disabled = true;
    const submit = root.querySelector('button[type="submit"]');
    if (submit) submit.disabled = true;
    try { target.value = await upload(input.files[0]); notify('图片已上传'); }
    catch (error) { showError(root, error); }
    finally {
      input.disabled = false; input.value = ''; if (submit) submit.disabled = false;
      formElement.dataset.uploadBusy = 'false'; fieldset.disabled = false;
    }
  });
}
export const uploadControl = name => `<label class="upload-control">上传 JPEG / PNG <input type="file" accept="image/jpeg,image/png" data-upload="${escape(name)}"></label>`;
