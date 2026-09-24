package top.egon.enjoyshop.goods.adapter.teaching.graphql;

import lombok.extern.slf4j.Slf4j;
import top.egon.enjoyshop.goods.adapter.teaching.pojo.convertor.TeachingAdapterConvertor;
import top.egon.enjoyshop.goods.adapter.teaching.pojo.vo.CourseDetailVO;
import top.egon.enjoyshop.goods.adapter.teaching.pojo.vo.SchoolClassDetailVO;
import top.egon.enjoyshop.goods.application.teaching.manage.CourseManage;
import top.egon.enjoyshop.goods.application.teaching.manage.SchoolClassManage;
import top.egon.enjoyshop.goods.application.teaching.pojo.query.GetCourseQuery;
import top.egon.enjoyshop.goods.application.teaching.pojo.query.GetSchoolClassQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
@Slf4j
public class CourseResolver {
    private final CourseManage courseManage;
    private final SchoolClassManage schoolClassManage;
    private final TeachingAdapterConvertor convertor;

    @QueryMapping
    public CourseDetailVO course(@Argument String id) {
        return convertor.toTarget(courseManage.get(new GetCourseQuery(Long.valueOf(id))));
    }

    @QueryMapping
    public SchoolClassDetailVO schoolClass(@Argument String id) {
        return convertor.toSchoolClassDetail(
                schoolClassManage.get(new GetSchoolClassQuery(Long.valueOf(id))));
    }
}
