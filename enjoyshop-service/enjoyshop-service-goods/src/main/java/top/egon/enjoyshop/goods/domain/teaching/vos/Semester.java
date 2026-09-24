package top.egon.enjoyshop.goods.domain.teaching.vos;

public record Semester(String value) {
    public Semester {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("semester must not be blank");
        }
    }
}
