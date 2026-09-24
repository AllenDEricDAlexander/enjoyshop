package top.egon.enjoyshop.goods.domain.user.service;

import jakarta.validation.constraints.NotBlank;
import top.egon.enjoyshop.goods.domain.user.vos.ExternalUser;

import java.util.Optional;

/** Outbound user query capability owned by the domain. */
public interface UserQueryService {
    Optional<ExternalUser> findExternalUser(@NotBlank String externalId);
}
