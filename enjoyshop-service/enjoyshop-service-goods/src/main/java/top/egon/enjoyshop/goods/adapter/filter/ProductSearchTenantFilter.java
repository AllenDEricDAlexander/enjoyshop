package top.egon.enjoyshop.goods.adapter.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.IDN;
import java.util.Locale;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import top.egon.cola.component.common.core.pojo.ResultRecord;
import top.egon.enjoyshop.goods.application.search.ProductSearchQueryConverter;
import top.egon.enjoyshop.goods.common.enums.GoodsErrorStatus;
import top.egon.enjoyshop.goods.common.config.ProductSearchProperties;

/** Resolves the public search site independently from legacy request-controlled tenant headers. */
@Component("productSearchTenantFilter")
@Order(Ordered.HIGHEST_PRECEDENCE + 2)
@Slf4j
@RequiredArgsConstructor
public class ProductSearchTenantFilter extends OncePerRequestFilter {
    private static final Pattern CANONICAL_HOST = Pattern.compile(
            "^(?=.{1,253}$)(?:[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?$");
    @Qualifier("productSearchProperties") private final ProductSearchProperties properties;
    @Qualifier("jacksonObjectMapper") private final ObjectMapper mapper;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        return !"GET".equals(request.getMethod()) || !"/api/v1/product-search".equals(path);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
        if (!properties.isEnabled()) {
            failure(response, 503, GoodsErrorStatus.SEARCH_UNAVAILABLE);
            return;
        }
        String host = canonicalHost(request.getServerName());
        Long tenant = host == null ? null : properties.getPublicSites().get(host);
        if (tenant == null || tenant <= 0) {
            log.debug("product search request has no registered public site");
            failure(response, 404, GoodsErrorStatus.SEARCH_SITE_NOT_CONFIGURED);
            return;
        }
        String previousTenant = MDC.get(TenantContextFilter.TENANT_MDC_KEY);
        String previousScope = MDC.get(ProductSearchQueryConverter.SEARCH_TENANT_MDC_KEY);
        MDC.put(TenantContextFilter.TENANT_MDC_KEY, tenant.toString());
        MDC.put(ProductSearchQueryConverter.SEARCH_TENANT_MDC_KEY, tenant.toString());
        try {
            chain.doFilter(request, response);
        } finally {
            restore(TenantContextFilter.TENANT_MDC_KEY, previousTenant);
            restore(ProductSearchQueryConverter.SEARCH_TENANT_MDC_KEY, previousScope);
        }
    }

    private static void restore(String key, String previous) {
        if (previous == null) { MDC.remove(key); } else { MDC.put(key, previous); }
    }

    private static String canonicalHost(String value) {
        if (value == null || value.isBlank()) { return null; }
        try {
            String host = value.endsWith(".") ? value.substring(0, value.length() - 1) : value;
            String ascii = IDN.toASCII(host, IDN.USE_STD3_ASCII_RULES).toLowerCase(Locale.ROOT);
            return CANONICAL_HOST.matcher(ascii).matches() ? ascii : null;
        } catch (IllegalArgumentException invalidHost) { return null; }
    }

    private void failure(HttpServletResponse response, int status, GoodsErrorStatus error) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        mapper.writeValue(response.getOutputStream(), ResultRecord.failure(error));
    }
}
