package top.egon.enjoyshop.goods.common.validation;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import top.egon.cola.platform.tianquan.jianshen.contract.authorization.SystemAuthorizationSnapshot;
import top.egon.cola.platform.tianquan.jianshen.starter.security.Rbac3AuthenticationToken;
import top.egon.cola.platform.tianquan.jianshen.starter.security.Rbac3UserDetails;
import top.egon.cola.platform.tianquan.shoubing.contract.AuthenticationContext;
import top.egon.cola.platform.tianquan.shoubing.contract.IdentityPrincipal;
import top.egon.enjoyshop.goods.application.goods.SpecListQuery;
import top.egon.enjoyshop.goods.application.goods.SpecManage;
import top.egon.enjoyshop.goods.domain.goods.SpecDomainQuery;
import top.egon.enjoyshop.goods.domain.goods.SpecDomainService;
import top.egon.enjoyshop.goods.infrastructure.goods.dao.SpecDAO;
import top.egon.enjoyshop.goods.infrastructure.goods.repo.SpecRepository;
import top.egon.enjoyshop.goods.start.StudentManagementApplication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ActiveProfiles("test")
@SpringBootTest(classes = StudentManagementApplication.class)
class SpecCatalogQueryValidationTest extends top.egon.enjoyshop.goods.support.PersistenceTestSupport {

    @Autowired
    @Qualifier("specDomainService")
    private SpecDomainService domainService;

    @Autowired
    @Qualifier("specManage")
    private SpecManage specManage;

    @Autowired
    @Qualifier("specRepository")
    private SpecRepository repository;

    @MockitoBean
    private SpecDAO specDAO;

    @BeforeEach
    void resetCollaborator() {
        reset(specDAO);
    }

    @AfterEach
    void clearVerifiedReader() {
        SecurityContextHolder.clearContext();
        MDC.clear();
    }

    @Test
    void domain_service_rejects_invalid_required_names_before_querying() {
        for (String categoryName : new String[]{null, "", "   ", "x".repeat(121)}) {
            assertThatThrownBy(() -> domainService.listByCategoryName(query(categoryName)))
                    .isInstanceOf(jakarta.validation.ConstraintViolationException.class);
        }

        verifyNoInteractions(specDAO);
    }

    @Test
    void application_manage_rejects_invalid_names_before_domain_and_persistence() {
        installSpecReader();
        for (String categoryName : new String[]{null, "", "   ", "x".repeat(121)}) {
            assertThatThrownBy(() -> specManage.listSpecsByCategoryName(
                    new SpecListQuery().setCategoryName(categoryName)))
                    .isInstanceOf(jakarta.validation.ConstraintViolationException.class);
        }

        verifyNoInteractions(specDAO);
    }

    @Test
    void repository_rejects_invalid_required_names_before_mapper_call() {
        for (String categoryName : new String[]{null, "", "   ", "x".repeat(121)}) {
            assertThatThrownBy(() -> repository.listByCategoryName(query(categoryName)))
                    .isInstanceOf(jakarta.validation.ConstraintViolationException.class);
        }

        verifyNoInteractions(specDAO);
    }

    @Test
    void valid_name_crosses_domain_and_repository_validation_groups() {
        when(specDAO.listByCategoryName("phones")).thenReturn(List.of());

        assertThat(domainService.listByCategoryName(query("phones"))).isEmpty();

        verify(specDAO).listByCategoryName("phones");
    }

    private static SpecDomainQuery query(String categoryName) {
        return new SpecDomainQuery().setCategoryName(categoryName);
    }

    private static void installSpecReader() {
        Instant now = Instant.parse("2026-10-09T00:00:00Z");
        IdentityPrincipal identity = new IdentityPrincipal(
                "spec-reader", "1", "test-jti", Set.of("platform"), now, now.plusSeconds(300),
                AuthenticationContext.password());
        SystemAuthorizationSnapshot snapshot = new SystemAuthorizationSnapshot(
                "1", "spec-reader", "rbac-user", "enjoyshop-goods", 1, 1,
                List.of(), List.of(), null, Set.of("enjoyshop:spec:read"), Set.of("enjoyshop.spec"),
                Map.of(), Map.of(), "test", now, now.plusSeconds(1));
        SecurityContextHolder.getContext().setAuthentication(
                new Rbac3AuthenticationToken(new Rbac3UserDetails(identity, snapshot)));
        MDC.put("tenantId", "1");
        MDC.put("userId", "spec-reader");
    }
}
