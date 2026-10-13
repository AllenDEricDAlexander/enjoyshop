import { api, segment } from '../api.js';
import { id } from '../contracts.js';
import { pageHeader, fields, field, form, onSubmit, detailList, escape, notify } from '../ui.js';

export function renderUsers(root) {
  root.innerHTML = pageHeader('用户与角色', '创建用户、按 ID 查询资料，并为已有用户分配角色。') +
    `<div class="two-columns"><section class="panel"><h2>创建用户</h2>${form('user-create', fields(
      field('externalId', '外部身份 ID', '', { required: true }) + field('name', '姓名', '', { required: true }) +
      field('email', '邮箱', '', { required: true, type: 'email' })
    ), '创建用户')}</section><section class="panel"><h2>查询用户</h2>${form('user-query', fields(field('userId', '用户 ID', '', { required: true, inputmode: 'numeric' })), '查询')}<div id="user-detail"></div></section></div>` +
    `<section class="panel"><h2>分配角色</h2>${form('role-assign', fields(
      field('userId', '用户 ID', '', { required: true, inputmode: 'numeric' }) + field('roleCode', '角色编码', '', { required: true })
    ), '分配角色')}<div id="role-result"></div></section>`;
  const showUser = (selector, user) => { root.querySelector(selector).innerHTML = detailList({ '用户 ID': user.id, '姓名': user.name, '邮箱': user.email, '状态': user.status }); };
  onSubmit(root.querySelector('#user-create'), async values => {
    const user = await api('/api/users', { method: 'POST', body: values });
    showUser('#user-detail', user);
    root.querySelector('#user-query [name="userId"]').value = user.id;
    root.querySelector('#role-assign [name="userId"]').value = user.id;
    notify('用户已创建');
  });
  onSubmit(root.querySelector('#user-query'), async ({ userId }) => showUser('#user-detail', await api(`/api/users/${segment(id(userId))}`)));
  onSubmit(root.querySelector('#role-assign'), async ({ userId, roleCode }) => {
    showUser('#role-result', await api(`/api/users/${segment(id(userId))}/roles`, { method: 'POST', body: { roleCode: roleCode.trim() } }));
    notify('角色已分配');
  });
}
export function permissionTree(nodes) {
  if (!nodes.length) return '<p class="muted">该用户暂无权限。</p>';
  const render = items => `<ul class="permission-tree">${items.map(item => `<li><span>${escape(item.name)} <code>${escape(item.code)}</code></span>${item.children?.length ? render(item.children) : ''}</li>`).join('')}</ul>`;
  return render(nodes);
}
export function renderPermissions(root) {
  root.innerHTML = pageHeader('权限管理', '查询用户权限树，向角色授予已有权限编码。') +
    `<div class="two-columns"><section class="panel"><h2>用户权限树</h2>${form('permission-query', fields(field('userId', '用户 ID', '', { required: true, inputmode: 'numeric' })), '查询权限')}<div id="permission-tree"></div></section>` +
    `<section class="panel"><h2>角色授权</h2>${form('permission-grant', fields(field('roleCode', '角色编码', '', { required: true }) + field('permissionCode', '权限编码', '', { required: true })), '授予权限')}<div id="grant-result"></div></section></div>`;
  onSubmit(root.querySelector('#permission-query'), async ({ userId }) => {
    const result = await api(`/api/users/${segment(id(userId))}/permissions`);
    root.querySelector('#permission-tree').innerHTML = permissionTree(result);
  });
  onSubmit(root.querySelector('#permission-grant'), async ({ roleCode, permissionCode }) => {
    const result = await api(`/api/roles/${segment(roleCode.trim())}/permissions`, { method: 'POST', body: { permissionCode: permissionCode.trim() } });
    root.querySelector('#grant-result').innerHTML = detailList({ '角色': result.roleCode, '权限': result.permissionCode, '状态': result.status });
    notify('权限已授予');
  });
}
