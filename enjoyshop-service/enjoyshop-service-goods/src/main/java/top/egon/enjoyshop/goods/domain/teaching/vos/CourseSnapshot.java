package top.egon.enjoyshop.goods.domain.teaching.vos;

import top.egon.enjoyshop.goods.domain.teaching.entities.Course;
import top.egon.enjoyshop.goods.domain.teaching.enums.CourseStatus;

public record CourseSnapshot(Long id, CourseCode code, String name, CourseStatus status) {
    public static CourseSnapshot from(Course course) {
        return new CourseSnapshot(course.id(), course.code(), course.name(), course.status());
    }

}
