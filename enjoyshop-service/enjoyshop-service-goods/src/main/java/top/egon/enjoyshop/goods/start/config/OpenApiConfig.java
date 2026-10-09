package top.egon.enjoyshop.goods.start.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(value = "openApiConfig", proxyBeanMethods = false)
public class OpenApiConfig {
    @Bean("studentManagementOpenApi")
    OpenAPI studentManagementOpenApi() {
        io.swagger.v3.oas.models.Components components = new io.swagger.v3.oas.models.Components();
        components.addSecuritySchemes("bearerAuth", new io.swagger.v3.oas.models.security.SecurityScheme()
                .type(io.swagger.v3.oas.models.security.SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT"));
        return new OpenAPI().components(components).info(new Info()
                .title("Student Management")
                .description("Lite monolith domain architecture sample API")
                .version("1.0.0"));
    }

}
