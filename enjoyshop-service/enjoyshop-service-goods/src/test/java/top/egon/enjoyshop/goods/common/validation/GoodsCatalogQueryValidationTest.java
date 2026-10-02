package top.egon.enjoyshop.goods.common.validation;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.convention.TestBean;
import top.egon.cola.component.common.core.pojo.PageQuery;
import top.egon.enjoyshop.goods.domain.goods.BrandDomainQuery;
import top.egon.enjoyshop.goods.domain.goods.BrandDomainService;
import top.egon.enjoyshop.goods.infrastructure.goods.dao.BrandDAO;
import top.egon.enjoyshop.goods.infrastructure.goods.repo.BrandRepository;
import top.egon.enjoyshop.goods.start.StudentManagementApplication;

@ActiveProfiles("test")
@SpringBootTest(
        classes = StudentManagementApplication.class,
        properties = {
                "app.integrations.rabbitmq.enabled=false",
                "app.integrations.redis.enabled=false",
                "app.integrations.external-http.enabled=false",
                "egon.cola.component.rpc.enabled=false",
                "egon.cola.component.tianshu.enabled=false"
        })
class GoodsCatalogQueryValidationTest extends top.egon.enjoyshop.goods.support.PersistenceTestSupport {

    @TestBean(name = "brandDAO", methodName = "mockBrandDAO", enforceOverride = true)
    private BrandDAO brandDAO;

    @Autowired
    @Qualifier("brandDomainService")
    private BrandDomainService brandDomainService;

    @Autowired
    @Qualifier("brandRepository")
    private BrandRepository brandRepository;

    static BrandDAO mockBrandDAO() {
        return mock(BrandDAO.class);
    }

    @Test
    void domainServiceRejectsInvalidGroupedCategoryBeforeRepositoryAccess() {
        assertThatThrownBy(() -> brandDomainService.listPage(invalidQuery(), null, null))
                .isInstanceOf(ConstraintViolationException.class);

        verifyNoInteractions(brandDAO);
    }

    @Test
    void repositoryRejectsInvalidGroupedCategoryBeforeMapperAccess() {
        assertThatThrownBy(() -> brandRepository.selectBrandPage(invalidQuery(), null, null))
                .isInstanceOf(ConstraintViolationException.class);

        verifyNoInteractions(brandDAO);
    }

    private static BrandDomainQuery invalidQuery() {
        return BrandDomainQuery.builder()
                .categoryName("x".repeat(121))
                .page(new PageQuery(1, 10))
                .build();
    }
}
