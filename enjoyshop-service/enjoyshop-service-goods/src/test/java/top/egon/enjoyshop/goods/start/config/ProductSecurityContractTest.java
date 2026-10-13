package top.egon.enjoyshop.goods.start.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.assertj.core.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.web.FilterChainProxy;
import top.egon.enjoyshop.goods.start.StudentManagementApplication;
import top.egon.enjoyshop.goods.support.PersistenceTestSupport;

@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest(classes = StudentManagementApplication.class)
class ProductSecurityContractTest extends PersistenceTestSupport {
    @Autowired private MockMvc mvc;
    @Autowired private FilterChainProxy chain;

    @Test void missingIdentityFailsClosedAndFiltersHaveTheRequiredOrder() throws Exception {
        mvc.perform(get("/api/v1/products")).andExpect(status().isUnauthorized());
        var names = chain.getFilters("/api/v1/products").stream().map(filter -> filter.getClass().getSimpleName()).toList();
        assertThat(names).containsSubsequence("IdpBearerAuthenticationFilter", "Rbac3BearerAuthenticationFilter", "AnonymousAuthenticationFilter");
        assertThat(chain.getFilters("/api/v1/brands").stream().map(filter -> filter.getClass().getSimpleName()).toList())
                .containsSubsequence("IdpBearerAuthenticationFilter", "Rbac3BearerAuthenticationFilter", "AnonymousAuthenticationFilter");
    }

    @Test void snapshotFailureKeepsTheFilterErrorShape() throws Exception {
        mvc.perform(get("/api/v1/products").header("Authorization", "Bearer product-unavailable"))
                .andExpect(status().isServiceUnavailable()).andExpect(jsonPath("message").exists())
                .andExpect(jsonPath("data").doesNotExist());
    }    @Test void missingRequiredIdentityBeanPreventsProductChainStartup() {
        new org.springframework.boot.test.context.runner.WebApplicationContextRunner()
                .withUserConfiguration(NativeHttpSecurityConfiguration.class)
                .withPropertyValues("app.web.cors.allowed-origins=https://client.example.test")
                .withBean("jacksonObjectMapper", com.fasterxml.jackson.databind.ObjectMapper.class, com.fasterxml.jackson.databind.ObjectMapper::new)
                .withBean(org.springframework.security.config.annotation.web.builders.HttpSecurity.class,
                        () -> org.mockito.Mockito.mock(org.springframework.security.config.annotation.web.builders.HttpSecurity.class, org.mockito.Mockito.RETURNS_SELF))
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure()).hasStackTraceContaining("idpBearerAuthenticationFilter");
                });
    }
    @Test void serviceIdentityAndUnavailableCacheOrFetchDoNotReachBusiness() throws Exception {
        mvc.perform(get("/api/v1/products").header("Authorization", "Bearer product-service")).andExpect(status().isUnauthorized());
        for (String token : new String[]{"product-cache-unavailable", "product-timeout"}) {
            mvc.perform(get("/api/v1/products").header("Authorization", "Bearer " + token))
                    .andExpect(status().isServiceUnavailable()).andExpect(jsonPath("data").doesNotExist());
        }
        assertThat(new org.springframework.jdbc.core.JdbcTemplate(testDataSource).queryForObject("SELECT COUNT(*) FROM spu", Integer.class)).isZero();
    }

}
