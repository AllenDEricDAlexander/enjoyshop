package top.egon.enjoyshop.goods.start.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import top.egon.cola.platform.tianquan.shoubing.starter.security.IdpBearerAuthenticationFilter;

/** Preserves existing public business routes after the OpenAPI starter adds Security. */
@Configuration(value = "nativeHttpSecurityConfiguration", proxyBeanMethods = false)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class NativeHttpSecurityConfiguration {

    private static final String FILE_UPLOAD_PATH = "/api/v1/files";

    @Bean("applicationSecurityFilterChain")
    @Order(Ordered.LOWEST_PRECEDENCE)
    SecurityFilterChain applicationSecurityFilterChain(HttpSecurity http,
            @Qualifier("goodsCorsConfigurationSource") CorsConfigurationSource corsConfigurationSource,
            @Qualifier("jacksonObjectMapper") ObjectMapper objectMapper)
            throws Exception {
        configureCommonSecurity(http, corsConfigurationSource, objectMapper);
        return http.authorizeHttpRequests(authorize -> authorize
                        .anyRequest().permitAll())
                .build();
    }

    @Bean("fileUploadSecurityFilterChain")
    @Order(Ordered.LOWEST_PRECEDENCE - 1)
    SecurityFilterChain fileUploadSecurityFilterChain(HttpSecurity http,
            @Qualifier("goodsCorsConfigurationSource") CorsConfigurationSource corsConfigurationSource,
            @Qualifier("jacksonObjectMapper") ObjectMapper objectMapper,
            @Qualifier("idpBearerAuthenticationFilter")
            ObjectProvider<IdpBearerAuthenticationFilter> shoubingFilterProvider,
            @Value("${egon.cola.platform.tianquan.shoubing.register-filter:false}")
            boolean servletFilterRegistration)
            throws Exception {
        if (servletFilterRegistration) {
            throw new IllegalStateException(
                    "Shoubing filter must be installed in the file-upload SecurityFilterChain");
        }

        http.securityMatcher(new AntPathRequestMatcher(FILE_UPLOAD_PATH, HttpMethod.POST.name()));
        configureCommonSecurity(http, corsConfigurationSource, objectMapper);
        IdpBearerAuthenticationFilter shoubingFilter = shoubingFilterProvider.getIfAvailable();
        if (shoubingFilter != null) {
            http.addFilterBefore(shoubingFilter, AnonymousAuthenticationFilter.class);
        }
        return http.authorizeHttpRequests(authorize -> authorize
                        .anyRequest().authenticated())
                .build();
    }

    private static void configureCommonSecurity(HttpSecurity http,
            CorsConfigurationSource corsConfigurationSource,
            ObjectMapper objectMapper) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .logout(logout -> logout.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .exceptionHandling(exceptions -> exceptions.authenticationEntryPoint((request, response, failure) -> {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    objectMapper.writeValue(response.getOutputStream(), Map.of(
                            "code", "AUTHORIZATION_HEADER_INVALID",
                            "message", "Tianquan-Shoubing authentication failed"));
                }));
    }

    @Bean("goodsCorsConfigurationSource")
    CorsConfigurationSource goodsCorsConfigurationSource(
            @Value("${app.web.cors.allowed-origins}") List<String> allowedOrigins) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(allowedOrigins);
        configuration.setAllowedMethods(List.of(HttpMethod.GET.name(), HttpMethod.POST.name(), HttpMethod.OPTIONS.name()));
        configuration.setAllowedHeaders(List.of(HttpHeaders.AUTHORIZATION, HttpHeaders.CONTENT_TYPE));
        configuration.setExposedHeaders(List.of(HttpHeaders.LOCATION));
        configuration.setAllowCredentials(false);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }
}
