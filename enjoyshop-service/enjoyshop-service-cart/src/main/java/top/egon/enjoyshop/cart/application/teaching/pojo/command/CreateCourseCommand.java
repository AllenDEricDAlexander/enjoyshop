package top.egon.enjoyshop.cart.application.teaching.pojo.command;

public record CreateCourseCommand(
        String code,
        String name,
        String operatorId,
        String idempotencyKey) {
}
