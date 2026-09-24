package top.egon.enjoyshop.goods.domain.user.aggregates;

import top.egon.enjoyshop.goods.domain.user.entities.Role;
import top.egon.enjoyshop.goods.domain.user.entities.User;
import top.egon.enjoyshop.goods.domain.user.enums.RoleStatus;
import top.egon.enjoyshop.goods.domain.user.enums.UserStatus;
import top.egon.enjoyshop.goods.common.exception.UserDomainException;
import top.egon.enjoyshop.goods.domain.user.vos.RoleCode;
import top.egon.enjoyshop.goods.domain.user.vos.UserId;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UserAggregateTest {
    @Test
    void assigns_active_role_to_active_user() {
        UserAggregate aggregate = new UserAggregate(
                new User(new UserId(1001L), "Mario", "mario@example.com", UserStatus.ACTIVE));

        aggregate.assign(new Role(new RoleCode("teacher"), "Teacher", RoleStatus.ACTIVE));

        assertEquals(1, aggregate.roles().size());
    }

    @Test
    void rejects_role_assignment_for_disabled_user() {
        UserAggregate aggregate = new UserAggregate(
                new User(new UserId(1001L), "Mario", "mario@example.com", UserStatus.DISABLED));
        Role role = new Role(new RoleCode("teacher"), "Teacher", RoleStatus.ACTIVE);

        assertThrows(UserDomainException.class, () -> aggregate.assign(role));
    }
}
