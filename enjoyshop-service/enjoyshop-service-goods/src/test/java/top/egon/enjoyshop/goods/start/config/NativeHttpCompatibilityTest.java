package top.egon.enjoyshop.goods.start.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import top.egon.enjoyshop.goods.application.user.manage.UserManage;
import top.egon.enjoyshop.goods.application.user.pojo.result.UserResult;
import top.egon.enjoyshop.goods.start.StudentManagementApplication;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest(classes = StudentManagementApplication.class)
class NativeHttpCompatibilityTest extends top.egon.enjoyshop.goods.support.PersistenceTestSupport {
    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private UserManage userManage;

    @Test
    void business_post_keeps_public_access_request_context_and_json_contract() throws Exception {
        when(userManage.create(any())).thenReturn(new UserResult(1001L, "Mario", "mario@example.com", "ACTIVE"));
        mockMvc.perform(post("/api/users").header("X-Operator-Id", "operator-1")
                        .header("X-Request-Id", "request-1").contentType("application/json")
                        .content("{\"externalId\":\"external-1\",\"name\":\"Mario\",\"email\":\"mario@example.com\"}"))
                // DEC-115 已删除自带 ResponseWrapperHandler/ApiResponse：样例域端点不再加信封，
                // 直接序列化 UserDetailVO（id 为 String 投影）。公开访问、请求上下文、JSON 契约三项保证不变。
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("1001"))
                .andExpect(jsonPath("$.name").value("Mario"))
                .andExpect(jsonPath("$.email").value("mario@example.com"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                // 信封确已消失：不再有 code/data 包装层（外形回归即失败）
                .andExpect(jsonPath("$.code").doesNotExist())
                .andExpect(jsonPath("$.data").doesNotExist());
        // 真实契约所在：请求头 X-Operator-Id/X-Request-Id 经 RequestContext 透传进 CreateUserCommand
        verify(userManage).create(new top.egon.enjoyshop.goods.application.user.pojo.command.CreateUserCommand(
                "external-1", "Mario", "mario@example.com", "operator-1", "request-1"));
    }

    @Test
    void platform_starter_retains_openapi_info_and_business_paths() throws Exception {
        mockMvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("Student Management"))
                .andExpect(jsonPath("$.paths['/api/users'].post").exists())
                .andExpect(jsonPath("$.paths['/api/courses'].post").exists())
                .andExpect(jsonPath("$.paths['/api/users/{userId}'].get").exists());
    }

    @org.junit.jupiter.api.Nested
    @SpringBootTest(classes = StudentManagementApplication.class, properties = {
            "egon.cola.component.yuheng.openapi.enabled=true",
            "egon.cola.component.yuheng.openapi.publish-to-tianshu=false",
            "egon.cola.component.yuheng.openapi.published-groups[0]=public",
            "egon.cola.component.yuheng.openapi.biz-code=student-management",
            "egon.cola.component.yuheng.openapi.application-code=light",
            "egon.cola.component.yuheng.openapi.resource-uri=https://light.example.test",
            "egon.cola.component.yuheng.openapi.artifact-version=1.0.0",
            "egon.cola.component.yuheng.openapi.build-id=contract-test",
            "springdoc.group-configs[0].group=public",
            "springdoc.group-configs[0].paths-to-match=/governed-probe",
            "egon.cola.component.yuheng.openapi.published-groups[1]=legacy",
            "springdoc.group-configs[1].group=legacy",
            "springdoc.group-configs[1].paths-to-match=/api/**"
    })
    @org.springframework.context.annotation.Import(JwtTestConfiguration.class)
    class DocumentGovernance {
        @Autowired
        private MockMvc governedMvc;

        @Test
        void documents_require_the_document_scope() throws Exception {
            governedMvc.perform(get("/v3/api-docs/public")).andExpect(status().isUnauthorized());
            governedMvc.perform(get("/v3/api-docs/public").header("Authorization", "Bearer other-token"))
                    .andExpect(status().isForbidden());
            governedMvc.perform(get("/v3/api-docs/public").header("Authorization", "Bearer docs-token"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.paths['/governed-probe'].get.operationId").value("governedProbe"));
        }

        @Test
        void uncatalogued_operations_require_explicit_operation_ids_before_publication() throws Exception {
            // DEC-115 已删除自带 GlobalExceptionHandler：治理拒绝不再被翻译成 400 + $.message 信封，
            // IllegalArgumentException 原样穿过 servlet 栈（FrameworkServlet 包成 ServletException）。
            // 治理保证本身不变——未编目 operationId 的分组在发布前被拒，且拒绝原因仍是该消息——故在根因处断言。
            jakarta.servlet.ServletException refusal = assertThrows(jakarta.servlet.ServletException.class,
                    () -> governedMvc.perform(get("/v3/api-docs/legacy")
                            .header("Authorization", "Bearer docs-token")));
            Throwable rootCause = org.springframework.core.NestedExceptionUtils.getMostSpecificCause(refusal);
            assertInstanceOf(IllegalArgumentException.class, rootCause);
            assertEquals("catalogued OpenAPI operationId is required", rootCause.getMessage());
        }

        @Test
        void document_governance_leaves_business_and_health_routes_accessible() throws Exception {
            governedMvc.perform(get("/actuator/health")).andExpect(status().isOk());
            governedMvc.perform(post("/api/users").contentType("application/json").content("{}"))
                    .andExpect(status().isBadRequest());
        }
    }

    @org.springframework.boot.test.context.TestConfiguration(proxyBeanMethods = false)
    static class JwtTestConfiguration {
        @org.springframework.context.annotation.Bean("governedProbeController")
        GovernedProbeController governedProbeController() {
            return new GovernedProbeController();
        }

        @org.springframework.context.annotation.Bean("testJwtDecoder")
        org.springframework.security.oauth2.jwt.JwtDecoder jwtDecoder() {
            return token -> org.springframework.security.oauth2.jwt.Jwt.withTokenValue(token)
                    .header("alg", "none").subject("test-reader")
                    .issuedAt(java.time.Instant.now()).expiresAt(java.time.Instant.now().plusSeconds(60))
                    .claim("scope", "docs-token".equals(token) ? "yuheng.openapi.read" : "unrelated")
                    .build();
        }
    }

    @org.springframework.boot.test.context.TestComponent
    @org.springframework.web.bind.annotation.RestController
    static class GovernedProbeController {
        @org.springframework.web.bind.annotation.GetMapping("/governed-probe")
        @io.swagger.v3.oas.annotations.Operation(operationId = "governedProbe")
        java.util.Map<String, String> probe() {
            return java.util.Map.of("status", "ok");
        }
    }
}
