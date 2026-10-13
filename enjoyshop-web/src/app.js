import { escape, pageHeader, empty } from './ui.js';
import { renderConnection, renderOverview } from './pages/connection.js';
import { renderProducts } from './pages/products.js';
import { renderBrands, renderSpecs, renderFiles } from './pages/catalog.js';
import { renderAds, renderShowcase } from './pages/ads.js';
import { renderSearch } from './pages/search.js';
import { renderUsers, renderPermissions } from './pages/users.js';
import { renderCourses, renderClasses } from './pages/teaching.js';

const groups = [
  ['工作台', [['overview', '工作概览', '◈'], ['connection', '连接与登录', '◎']]],
  ['商品中心', [['products', '商品管理', '▦'], ['recycle', '商品回收站', '↺'], ['brands', '品牌管理', '◇'], ['specs', '分类规格', '≡'], ['search', '商城搜索', '⌕'], ['files', '图片上传', '↑']]],
  ['内容运营', [['ads', '广告管理', '▣'], ['showcase', '广告展示', '▧']]],
  ['组织与教学', [['users', '用户与角色', '♙'], ['permissions', '权限管理', '⚿'], ['courses', '课程管理', '▤'], ['classes', '班级与排课', '▥']]],
];
const routes = {
  overview: renderOverview, connection: renderConnection, products: renderProducts,
  recycle: root => renderProducts(root, true), brands: renderBrands, specs: renderSpecs,
  search: renderSearch, files: renderFiles, ads: renderAds, showcase: renderShowcase,
  users: renderUsers, permissions: renderPermissions, courses: renderCourses, classes: renderClasses,
};
const navigation = document.querySelector('#navigation');
navigation.innerHTML = groups.map(([title, items]) => `<div class="nav-group"><div class="nav-title">${title}</div>${items.map(([key, label, icon]) => `<a href="#/${key}" data-route="${key}"><span aria-hidden="true">${icon}</span>${label}</a>`).join('')}</div>`).join('');
function render() {
  const key = location.hash.replace(/^#\//, '').split('?')[0] || 'overview';
  const title = groups.flatMap(([, items]) => items).find(([route]) => route === key)?.[1] || '页面未找到';
  document.title = `${title} · EnjoyShop`;
  document.querySelector('#breadcrumb').textContent = `EnjoyShop / ${title}`;
  navigation.querySelectorAll('a').forEach(link => {
    link.classList.toggle('active', link.dataset.route === key);
    if (link.dataset.route === key) link.setAttribute('aria-current', 'page'); else link.removeAttribute('aria-current');
  });
  const dialog = document.querySelector('#editor');
  if (dialog.open && !dialog.querySelector('[data-busy="true"], [data-upload-busy="true"]')) dialog.close();
  // 每次路由创建新的节点，使旧请求不能覆盖新页面。
  const content = document.querySelector('#content');
  content.innerHTML = '<section class="page"></section>';
  const root = content.firstElementChild;
  try {
    if (routes[key]) routes[key](root);
    else root.innerHTML = pageHeader('页面未找到', '请从左侧导航选择功能。') + empty('路径不存在', escape(key));
  } catch (error) {
    root.textContent = `页面初始化失败：${error.message}`;
  }
  content.focus({ preventScroll: true });
}
window.addEventListener('hashchange', render);
render();
