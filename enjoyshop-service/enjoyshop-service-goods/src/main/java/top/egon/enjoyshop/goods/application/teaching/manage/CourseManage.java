package top.egon.enjoyshop.goods.application.teaching.manage;

import top.egon.enjoyshop.goods.application.teaching.pojo.command.CreateCourseCommand;
import top.egon.enjoyshop.goods.application.teaching.pojo.query.GetCourseQuery;
import top.egon.enjoyshop.goods.application.teaching.pojo.result.CourseResult;

public interface CourseManage {
    CourseResult create(CreateCourseCommand command);

    CourseResult get(GetCourseQuery query);
}
