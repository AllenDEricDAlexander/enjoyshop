import { api, segment } from '../api.js';
import { id } from '../contracts.js';
import { pageHeader, fields, field, form, onSubmit, detailList, notify } from '../ui.js';

export function renderCourses(root) {
  root.innerHTML = pageHeader('课程管理', '创建课程，并按课程 ID 查看资料。') +
    `<div class="two-columns"><section class="panel"><h2>创建课程</h2>${form('course-create', fields(field('code', '课程编码', '', { required: true }) + field('name', '课程名称', '', { required: true })), '创建课程')}</section>` +
    `<section class="panel"><h2>查询课程</h2>${form('course-query', fields(field('courseId', '课程 ID', '', { required: true, inputmode: 'numeric' })), '查询')}<div id="course-result"></div></section></div>`;
  const show = course => { root.querySelector('#course-result').innerHTML = detailList({ '课程 ID': course.id, '课程编码': course.code, '课程名称': course.name, '状态': course.status }); };
  onSubmit(root.querySelector('#course-create'), async values => {
    const course = await api('/api/courses', { method: 'POST', body: values });
    show(course);
    root.querySelector('#course-query [name="courseId"]').value = course.id;
    notify('课程已创建');
  });
  onSubmit(root.querySelector('#course-query'), async ({ courseId }) => show(await api(`/api/courses/${segment(id(courseId))}`)));
}
export function scheduleBody(values) {
  if (!values.startsAt || !values.endsAt || values.endsAt <= values.startsAt) throw new Error('结束时间必须晚于开始时间');
  // 后端为 LocalDateTime，保留当地墙上时间，不转换为 UTC。
  return { startsAt: values.startsAt.length === 16 ? `${values.startsAt}:00` : values.startsAt,
    endsAt: values.endsAt.length === 16 ? `${values.endsAt}:00` : values.endsAt };
}
export function renderClasses(root) {
  root.innerHTML = pageHeader('班级与排课', '创建班级，查询班级信息，并安排课程时间。') +
    `<div class="two-columns"><section class="panel"><h2>创建班级</h2>${form('class-create', fields(field('name', '班级名称', '', { required: true }) + field('semester', '学期', '', { required: true, placeholder: '例如：2026 秋季' })), '创建班级')}</section>` +
    `<section class="panel"><h2>查询班级</h2>${form('class-query', fields(field('schoolClassId', '班级 ID', '', { required: true, inputmode: 'numeric' })), '查询')}<div id="class-result"></div></section></div>` +
    `<section class="panel"><h2>安排课程</h2>${form('class-schedule', fields(field('schoolClassId', '班级 ID', '', { required: true, inputmode: 'numeric' }) + field('courseId', '课程 ID', '', { required: true, inputmode: 'numeric' }) + field('startsAt', '开始时间', '', { required: true, type: 'datetime-local', hint: '按业务所在地的当地时间填写' }) + field('endsAt', '结束时间', '', { required: true, type: 'datetime-local' })), '安排课程')}<div id="schedule-result"></div></section>`;
  const show = (selector, result) => { root.querySelector(selector).innerHTML = detailList({ '班级 ID': result.id, '班级': result.name, '学期': result.semester, '状态': result.status, '已排课程数': result.scheduleCount }); };
  onSubmit(root.querySelector('#class-create'), async values => {
    const result = await api('/api/school-classes', { method: 'POST', body: values });
    show('#class-result', result);
    root.querySelector('#class-query [name="schoolClassId"]').value = result.id;
    root.querySelector('#class-schedule [name="schoolClassId"]').value = result.id;
    notify('班级已创建');
  });
  onSubmit(root.querySelector('#class-query'), async ({ schoolClassId }) => show('#class-result', await api(`/api/school-classes/${segment(id(schoolClassId))}`)));
  onSubmit(root.querySelector('#class-schedule'), async values => {
    const result = await api(`/api/school-classes/${segment(id(values.schoolClassId))}/courses/${segment(id(values.courseId))}/schedule`, { method: 'POST', body: scheduleBody(values) });
    show('#schedule-result', result);
    notify('排课已完成');
  });
}
