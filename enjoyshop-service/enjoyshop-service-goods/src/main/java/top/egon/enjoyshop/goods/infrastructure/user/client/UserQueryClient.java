package top.egon.enjoyshop.goods.infrastructure.user.client;

import jakarta.validation.constraints.NotBlank;
import top.egon.enjoyshop.goods.domain.user.vos.ExternalUser;

import java.util.Optional;

/** Transport-facing user query client; profiles bind a local or HTTP implementation. */
public interface UserQueryClient {
    Optional<ExternalUser> findExternalUser(@NotBlank String externalId);
}
