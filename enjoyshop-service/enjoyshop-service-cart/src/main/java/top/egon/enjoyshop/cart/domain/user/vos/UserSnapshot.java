package top.egon.enjoyshop.cart.domain.user.vos;

import top.egon.enjoyshop.cart.domain.user.entities.User;
import top.egon.enjoyshop.cart.domain.user.enums.UserStatus;

public record UserSnapshot(Long id, String name, String email, UserStatus status) {
    public static UserSnapshot from(User user) {
        return new UserSnapshot(user.id().value(), user.name(), user.email(), user.status());
    }

}
