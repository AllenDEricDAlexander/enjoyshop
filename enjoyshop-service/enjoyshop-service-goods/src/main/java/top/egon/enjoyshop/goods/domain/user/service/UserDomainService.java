package top.egon.enjoyshop.goods.domain.user.service;

import top.egon.enjoyshop.goods.domain.user.aggregates.UserAggregate;
import top.egon.enjoyshop.goods.domain.user.entities.User;
import top.egon.enjoyshop.goods.domain.user.vos.UserId;

import java.util.Optional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/** Persistence-owning user domain service contract. */
public interface UserDomainService {
    User createUser(String externalId, String name, String email);

    User save(@Valid @NotNull User user);

    Optional<User> findById(@Valid @NotNull UserId userId);

    void saveRoles(@Valid @NotNull UserAggregate aggregate);
}
