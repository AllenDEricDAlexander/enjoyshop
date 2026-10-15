package top.egon.enjoyshop.cart.application.teaching.pojo.command;

public record CreateSchoolClassCommand(
        String name,
        String semester,
        String operatorId,
        String idempotencyKey) {
}
