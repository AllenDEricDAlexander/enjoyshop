package top.egon.enjoyshop.cart.application.teaching.pojo.query;

public record GetCourseQuery(Long courseId) {
    public GetCourseQuery {
        if (courseId == null || courseId <= 0) {
            throw new IllegalArgumentException("courseId must be positive");
        }
    }

}
