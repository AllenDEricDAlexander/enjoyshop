package top.egon.enjoyshop.cart.infrastructure.user.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import top.egon.enjoyshop.cart.domain.user.service.UserQueryService;
import top.egon.enjoyshop.cart.domain.user.vos.ExternalUser;
import top.egon.enjoyshop.cart.infrastructure.user.client.UserQueryClient;

import java.util.Optional;

/** User query capability; transport is delegated to the bound client implementation. */
@Validated
@Service("userQueryService")
@RequiredArgsConstructor
@Slf4j
public class UserQueryServiceImpl implements UserQueryService {
    @Qualifier("userQueryClient")
    private final UserQueryClient userQueryClient;

    @Override
    public Optional<ExternalUser> findExternalUser(String externalId) {
        return userQueryClient.findExternalUser(externalId);
    }
}
