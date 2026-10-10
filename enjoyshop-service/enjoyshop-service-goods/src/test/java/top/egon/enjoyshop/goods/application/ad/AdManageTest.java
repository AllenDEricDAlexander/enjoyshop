package top.egon.enjoyshop.goods.application.ad;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.security.core.context.SecurityContextHolder;
import top.egon.cola.component.common.core.exception.CommonException;
import top.egon.cola.component.common.core.validation.ValidationUtils;
import top.egon.cola.platform.tianquan.jianshen.contract.authorization.AuthorizationDecision;
import top.egon.cola.platform.tianquan.jianshen.contract.authorization.Decision;
import top.egon.cola.platform.tianquan.jianshen.contract.authorization.PermissionRequest;
import top.egon.cola.platform.tianquan.jianshen.contract.authorization.SystemAuthorizationSnapshot;
import top.egon.cola.platform.tianquan.jianshen.starter.authorization.AuthorizationService;
import top.egon.cola.platform.tianquan.jianshen.starter.security.Rbac3AuthenticationToken;
import top.egon.cola.platform.tianquan.jianshen.starter.security.Rbac3ContextAuthentication;
import top.egon.cola.platform.tianquan.jianshen.starter.security.Rbac3UserDetails;
import top.egon.cola.platform.tianquan.shoubing.contract.AuthenticationContext;
import top.egon.cola.platform.tianquan.shoubing.contract.IdentityPrincipal;
import top.egon.cola.platform.tianquan.shoubing.contract.PrincipalType;
import top.egon.enjoyshop.goods.common.validation.AdPositionValidator;
import top.egon.enjoyshop.goods.common.validation.AdUrlValidator;
import top.egon.enjoyshop.goods.common.validation.AdValidationGroup;
import top.egon.enjoyshop.goods.common.validation.AdWindowValidator;
import top.egon.enjoyshop.goods.common.validation.ValidAdPosition;
import top.egon.enjoyshop.goods.common.validation.ValidAdUrl;
import top.egon.enjoyshop.goods.common.validation.ValidAdWindow;
import top.egon.enjoyshop.goods.domain.ad.AdBO;
import top.egon.enjoyshop.goods.domain.ad.AdEligibilitySpecification;
import top.egon.enjoyshop.goods.domain.ad.AdStatusEnum;
import top.egon.enjoyshop.goods.infrastructure.ad.converter.AdPersistenceConverter;
import top.egon.enjoyshop.goods.infrastructure.ad.po.AdPO;
import top.egon.enjoyshop.goods.infrastructure.ad.repo.AdRepository;
import top.egon.enjoyshop.goods.infrastructure.config.AdProperties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdManageTest {

    @AfterEach
    void clearSecurityAndMdc() {
        SecurityContextHolder.clearContext();
        MDC.clear();
    }

    @Test
    void writesRejectUnknownTenantAndUseOperationGroups() throws Exception {
        AdSaveCommand command = new AdSaveCommand();
        assertThat(command.getClass().getDeclaredField("version").getAnnotation(jakarta.validation.constraints.Null.class))
                .isNotNull();
        assertThat(command.getClass().getDeclaredFields()).noneMatch(field -> field.getName().equals("tenantId"));
        assertThat(command.getClass().getDeclaredFields()).noneMatch(field -> field.getName().equals("id"));
        assertThatThrownBy(() -> command.rejectUnknown("tenantId", 42L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown advertisement field");
        assertThat(AdValidationGroup.Create.class.getInterfaces()).contains(AdValidationGroup.Write.class);
        assertThat(AdValidationGroup.Update.class.getInterfaces()).contains(AdValidationGroup.Write.class);
    }

    @Test
    void permissionsFailClosedAndMatchVerifiedUserContext() throws Exception {
        AuthorizationService decisions = mock(AuthorizationService.class);
        AdAuthorizationComponent gate = new AdAuthorizationComponent(decisions);
        assertThatThrownBy(() -> gate.requirePermission("enjoyshop:ad:read"))
                .isInstanceOf(CommonException.class)
                .extracting(failure -> ((CommonException) failure).getCode())
                .isEqualTo(401000);

        installVerifiedUser("41");
        for (String action : List.of("read", "write")) {
            String code = "enjoyshop:ad:" + action;
            PermissionRequest request = PermissionRequest.api(code, "enjoyshop.ad");
            when(decisions.requirePermission(request)).thenReturn(decision(code, Decision.ALLOW));
            gate.requirePermission(code);
            verify(decisions).requirePermission(request);
            when(decisions.requirePermission(request)).thenReturn(decision(code, Decision.DENY));
            assertAuthorizationCode(() -> gate.requirePermission(code), 610305);
            when(decisions.requirePermission(request)).thenReturn(decision(code, Decision.INDETERMINATE));
            assertAuthorizationCode(() -> gate.requirePermission(code), 610308);
            when(decisions.requirePermission(request)).thenReturn(null);
            assertAuthorizationCode(() -> gate.requirePermission(code), 610308);
        }
        MDC.put("tenantId", "42");
        assertAuthorizationCode(() -> gate.requirePermission("enjoyshop:ad:read"), 610305);
    }

    @Test
    void staleReplaceConflictsBeforeUpdate() {
        MDC.put("tenantId", "41");
        AdRepository repository = mock(AdRepository.class);
        AdPersistenceConverter converter = mock(AdPersistenceConverter.class);
        AdPO persisted = new AdPO();
        persisted.setId(900L);
        persisted.setVersion(3L);
        when(repository.lockByLifecycle(any())).thenReturn(persisted);
        var service = domainService(repository, converter);

        assertThatThrownBy(() -> service.replace(AdBO.builder().id(900L).version(2L).build()))
                .isInstanceOf(CommonException.class)
                .extracting(failure -> ((CommonException) failure).getCode())
                .isEqualTo(610303);
        verify(repository, never()).updateById(any(AdPO.class));
    }

    @Test
    void deleteOfMissingOrAlreadyDeletedAdIsTerminalSuccess() {
        MDC.put("tenantId", "41");
        AdRepository repository = mock(AdRepository.class);
        AdPersistenceConverter converter = mock(AdPersistenceConverter.class);
        when(repository.lockByLifecycle(any())).thenReturn(null);
        var service = domainService(repository, converter);

        assertThat(service.delete(AdBO.builder().id(900L).version(3L).build())).isTrue();
        verify(repository, never()).removeById(any(AdPO.class));
    }

    @Test
    void statusEnumUsesStrictStringCodes() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        assertThat(mapper.writeValueAsString(AdStatusEnum.ENABLED)).isEqualTo("\"1\"");
        assertThat(mapper.readValue("\"0\"", AdStatusEnum.class)).isEqualTo(AdStatusEnum.DISABLED);
        assertThatThrownBy(() -> mapper.readValue("1", AdStatusEnum.class))
                .hasRootCauseInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> mapper.readValue("\"2\"", AdStatusEnum.class))
                .hasRootCauseInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void windowValidatorRejectsNonIncreasingAndSubMicrosecondTimes() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            assertThat(validator.validate(new AdWindowProbe(
                    Instant.parse("2026-10-10T00:00:00Z"), Instant.parse("2026-10-10T00:00:00Z"))))
                    .extracting(violation -> violation.getPropertyPath().toString())
                    .containsExactly("endAt");
            assertThat(validator.validate(new AdWindowProbe(
                    Instant.parse("2026-10-10T00:00:00.000000001Z"), Instant.parse("2026-10-10T00:00:01Z"))))
                    .extracting(violation -> violation.getPropertyPath().toString())
                    .containsExactly("startAt");
            assertThat(validator.validate(new AdWindowProbe(
                    Instant.parse("2026-10-10T00:00:00.000001Z"), Instant.parse("2026-10-10T00:00:01Z"))))
                    .isEmpty();
        }
    }

    @Test
    void positionValidatorRequiresRegisteredCanonicalPosition() throws Exception {
        ValidAdPosition annotation = PositionProbe.class.getDeclaredField("value").getAnnotation(ValidAdPosition.class);
        AdPositionValidator validator = new AdPositionValidator(List.of("web_index_lb"));
        validator.initialize(annotation);
        var context = mock(jakarta.validation.ConstraintValidatorContext.class);
        assertThat(validator.isValid("web_index_lb", context)).isTrue();
        assertThat(validator.isValid("web_index_mobile", context)).isFalse();
        assertThat(validator.isValid("Web_index_lb", context)).isFalse();
        assertThat(validator.isValid("../web_index_lb", context)).isFalse();
    }

    @Test
    void urlValidatorRejectsUntrustedImageBasesAndUnsafeTargets() throws Exception {
        var context = mock(jakarta.validation.ConstraintValidatorContext.class);
        ValidAdUrl image = ImageUrlProbe.class.getDeclaredField("value").getAnnotation(ValidAdUrl.class);
        AdUrlValidator imageValidator = new AdUrlValidator(URI.create("https://cdn.example.test/product-media/"));
        imageValidator.initialize(image);
        assertThat(imageValidator.isValid("https://cdn.example.test/product-media/ad.png", context)).isTrue();
        assertThat(imageValidator.isValid("https://other.example.test/product-media/ad.png", context)).isFalse();
        assertThat(imageValidator.isValid("https://cdn.example.test/product-media-evil/ad.png", context)).isFalse();
        assertThat(imageValidator.isValid("https://cdn.example.test/product-media/%2e%2e/private.png", context)).isFalse();

        ValidAdUrl target = TargetUrlProbe.class.getDeclaredField("value").getAnnotation(ValidAdUrl.class);
        AdUrlValidator targetValidator = new AdUrlValidator(URI.create(""));
        targetValidator.initialize(target);
        assertThat(targetValidator.isValid("/activities/spring", context)).isTrue();
        assertThat(targetValidator.isValid("https://activities.example.test/spring", context)).isTrue();
        assertThat(targetValidator.isValid("//activities.example.test/spring", context)).isFalse();
        assertThat(targetValidator.isValid("javascript:alert(1)", context)).isFalse();
        assertThat(targetValidator.isValid("/activities/spring?token=secret", context)).isFalse();
    }

    @Test
    void eligibilityUsesStartInclusiveAndEndExclusiveWindow() {
        AdEligibilitySpecification specification = new AdEligibilitySpecification();
        Instant now = Instant.parse("2026-10-10T00:00:00Z");
        AdBO ad = AdBO.builder().status(AdStatusEnum.ENABLED).startAt(now).endAt(now.plusSeconds(1)).build();
        assertThat(specification.isEligible(ad, now)).isTrue();
        assertThat(specification.isEligible(ad, now.plusSeconds(1))).isFalse();
        assertThat(specification.isEligible(ad.setStatus(AdStatusEnum.DISABLED), now)).isFalse();
    }

    private static top.egon.enjoyshop.goods.infrastructure.ad.service.impl.AdDomainServiceImpl domainService(
            AdRepository repository, AdPersistenceConverter converter) {
        Validator validator = mock(Validator.class);
        doReturn(Set.of()).when(validator).validate(any(), any(Class[].class));
        return new top.egon.enjoyshop.goods.infrastructure.ad.service.impl.AdDomainServiceImpl(
                repository, converter, new ValidationUtils(validator), AdProperties.builder().build());
    }

    private static AuthorizationDecision decision(String code, Decision decision) {
        return new AuthorizationDecision(decision, "TEST", "41", "rbac-user", code, 1, 1, List.of(), Instant.now());
    }

    private static void installVerifiedUser(String tenant) {
        Instant now = Instant.now();
        IdentityPrincipal identity = new IdentityPrincipal("editor", tenant, "test-jti", Set.of("platform"), now,
                now.plusSeconds(300), AuthenticationContext.password());
        SystemAuthorizationSnapshot snapshot = new SystemAuthorizationSnapshot(tenant, "editor", "rbac-user",
                "enjoyshop-goods", 1, 1, List.of(), List.of(), null, Set.of(), Set.of("enjoyshop.ad"), Map.of(),
                Map.of(), "test", now, now.plusSeconds(1));
        SecurityContextHolder.getContext().setAuthentication(new Rbac3AuthenticationToken(new Rbac3UserDetails(identity, snapshot)));
        MDC.put("tenantId", tenant);
        MDC.put("userId", "editor");
    }

    private static void assertAuthorizationCode(org.assertj.core.api.ThrowableAssert.ThrowingCallable operation, int code) {
        assertThatThrownBy(operation)
                .isInstanceOf(CommonException.class)
                .extracting(failure -> ((CommonException) failure).getCode())
                .isEqualTo(code);
    }

    @ValidAdWindow
    private static final class AdWindowProbe implements AdWindowValidator.AdWindowInput {
        private final Instant startAt;
        private final Instant endAt;

        private AdWindowProbe(Instant startAt, Instant endAt) {
            this.startAt = startAt;
            this.endAt = endAt;
        }

        @Override
        public Instant getStartAt() {
            return startAt;
        }

        @Override
        public Instant getEndAt() {
            return endAt;
        }
    }

    private static final class PositionProbe {
        @ValidAdPosition
        private String value;
    }

    private static final class ImageUrlProbe {
        @ValidAdUrl(image = true)
        private String value;
    }

    private static final class TargetUrlProbe {
        @ValidAdUrl
        private String value;
    }
}
