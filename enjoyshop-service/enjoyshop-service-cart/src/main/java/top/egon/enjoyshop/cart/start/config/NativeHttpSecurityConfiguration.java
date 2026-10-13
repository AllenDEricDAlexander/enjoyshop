package top.egon.enjoyshop.cart.start.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;
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
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import top.egon.cola.platform.tianquan.jianshen.starter.security.Rbac3BearerAuthenticationFilter;
import top.egon.cola.platform.tianquan.shoubing.starter.security.IdpBearerAuthenticationFilter;

/** Preserves non-cart routes after the cart chain requires a verified bearer. */
@Configuration(value = "nativeHttpSecurityConfiguration", proxyBeanMethods = false)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class NativeHttpSecurityConfiguration {

    @Bean("applicationSecurityFilterChain")
    @Order(Ordered.LOWEST_PRECEDENCE)
    SecurityFilterChain applicationSecurityFilterChain(HttpSecurity http) throws Exception {
        return http.csrf(csrf -> csrf.disable())
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .logout(logout -> logout.disable())
                .authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll())
                .build();
    }

    @Bean("cartSecurityFilterChain")
    @Order(Ordered.LOWEST_PRECEDENCE - 1)
    SecurityFilterChain cartSecurityFilterChain(HttpSecurity http,
            @Qualifier("cartCorsConfigurationSource") CorsConfigurationSource cors,
            @Qualifier("jacksonObjectMapper") ObjectMapper mapper,
            @Qualifier("idpBearerAuthenticationFilter") IdpBearerAuthenticationFilter identity,
            @Qualifier("rbac3BearerAuthenticationFilter") Rbac3BearerAuthenticationFilter permissions,
            @Value("${egon.cola.platform.tianquan.shoubing.register-filter:false}") boolean idpRegistration,
            @Value("${egon.cola.platform.tianquan.jianshen.register-filter:false}") boolean rbacRegistration) throws Exception {
        if (idpRegistration || rbacRegistration) {
            throw new IllegalStateException("Cart authentication filters must only run inside the cart SecurityFilterChain");
        }
        http.securityMatcher("/api/v1/cart", "/api/v1/cart/**");
        configureCommonSecurity(http, cors, mapper);
        http.addFilterBefore(permissions, AnonymousAuthenticationFilter.class)
                .addFilterBefore(identity, Rbac3BearerAuthenticationFilter.class);
        return http.authorizeHttpRequests(authorize -> authorize.anyRequest().authenticated()).build();
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

    @Bean("cartCorsConfigurationSource")
    CorsConfigurationSource cartCorsConfigurationSource(
            @Value("${app.web.cors.allowed-origins}") List<String> allowedOrigins) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(allowedOrigins);
        configuration.setAllowedMethods(List.of(
                HttpMethod.GET.name(),
                HttpMethod.POST.name(),
                HttpMethod.PUT.name(),
                HttpMethod.DELETE.name(),
                HttpMethod.OPTIONS.name()));
        configuration.setAllowedHeaders(List.of(HttpHeaders.AUTHORIZATION, HttpHeaders.CONTENT_TYPE));
        configuration.setExposedHeaders(List.of(HttpHeaders.LOCATION));
        configuration.setAllowCredentials(false);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }
}
