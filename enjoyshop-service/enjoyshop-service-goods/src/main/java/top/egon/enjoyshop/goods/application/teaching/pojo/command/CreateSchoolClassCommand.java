package top.egon.enjoyshop.goods.application.teaching.pojo.command;

public record CreateSchoolClassCommand(
        String name,
        String semester,
        String operatorId,
        String idempotencyKey) {
}
