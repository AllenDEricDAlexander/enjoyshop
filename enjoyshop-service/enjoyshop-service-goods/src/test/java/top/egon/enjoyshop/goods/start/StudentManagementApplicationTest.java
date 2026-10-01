package top.egon.enjoyshop.goods.start;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import top.egon.enjoyshop.goods.domain.goods.BrandDomainService;

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
class StudentManagementApplicationTest extends top.egon.enjoyshop.goods.support.PersistenceTestSupport {

    @Autowired
    @Qualifier("brandDomainService")
    private BrandDomainService brandDomainService;

    @Test
    void contextLoads() {
    }

    @Test
    void shouldRejectInvalidDomainArgumentsAtTheValidatedInfrastructureProxy() {
        ConstraintViolationException exception = assertThrows(
                ConstraintViolationException.class,
                () -> brandDomainService.delete(null, -1L));

        assertThat(exception.getConstraintViolations()).hasSize(2);
    }
}
