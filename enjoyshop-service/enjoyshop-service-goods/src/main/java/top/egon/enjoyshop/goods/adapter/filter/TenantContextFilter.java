package top.egon.enjoyshop.goods.adapter.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * The only writer of the MDC keys MP-SDJ reads ({@code tenantId} for the tenant interceptor,
 * {@code userId} for audit fill and for the operator guard of Step 6).
 *
 * <p>The light archetype's own {@link RequestContextFilter} publishes a ThreadLocal view with
 * {@code X-Operator-Id} and never touches MDC, and {@link TraceIdFilter} writes only
 * {@code traceId}; without this filter {@code EgonColaTenantIdProvider} would raise
 * {@code TENANT_CONTEXT_MISSING} on every brand statement (Spec {@code EVD-012}, {@code EVD-027},
 * §7.3.1 step 1, {@code DEC-116}/{@code PLAN-CLAR-014}).</p>
 *
 * <p>Resolution never fails a request: a missing or blank {@code X-Tenant-Id} falls back to the
 * single-tenant default of {@code DEC-107}, and a missing {@code X-Actor-Id} stays empty so reads
 * keep working while writes are rejected downstream with 610105. Clearing happens in {@code finally}
 * because the servlet container reuses threads, and a leaked key would silently scope one caller's
 * request with another caller's tenant.</p>
 */
@Slf4j
@Component("tenantContextFilter")
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class TenantContextFilter extends OncePerRequestFilter {

    public static final String TENANT_HEADER = "X-Tenant-Id";
    public static final String OPERATOR_HEADER = "X-Actor-Id";
    public static final String DEFAULT_TENANT_ID = "1";

    static final String TENANT_MDC_KEY = "tenantId";
    static final String OPERATOR_MDC_KEY = "userId";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String tenantId = headerOrDefault(request, TENANT_HEADER, DEFAULT_TENANT_ID);
        String operatorId = headerOrDefault(request, OPERATOR_HEADER, "");
        MDC.put(TENANT_MDC_KEY, tenantId);
        MDC.put(OPERATOR_MDC_KEY, operatorId);
        if (log.isDebugEnabled()) {
            log.debug("request context tenant={} operator_present={}", tenantId, !operatorId.isEmpty());
        }
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(TENANT_MDC_KEY);
            MDC.remove(OPERATOR_MDC_KEY);
        }
    }

    private static String headerOrDefault(HttpServletRequest request, String header, String fallback) {
        String value = request.getHeader(header);
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
