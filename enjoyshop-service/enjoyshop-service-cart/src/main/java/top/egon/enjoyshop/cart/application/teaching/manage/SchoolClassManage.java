package top.egon.enjoyshop.cart.application.teaching.manage;

import top.egon.enjoyshop.cart.application.teaching.pojo.command.CreateSchoolClassCommand;
import top.egon.enjoyshop.cart.application.teaching.pojo.command.ScheduleCourseCommand;
import top.egon.enjoyshop.cart.application.teaching.pojo.query.GetSchoolClassQuery;
import top.egon.enjoyshop.cart.application.teaching.pojo.result.SchoolClassResult;

public interface SchoolClassManage {
    SchoolClassResult create(CreateSchoolClassCommand command);

    SchoolClassResult schedule(ScheduleCourseCommand command);

    SchoolClassResult get(GetSchoolClassQuery query);
}
