package top.egon.enjoyshop.goods.application.teaching.pojo.command;

public record CreateCourseCommand(
        String code,
        String name,
        String operatorId,
        String idempotencyKey) {
}
