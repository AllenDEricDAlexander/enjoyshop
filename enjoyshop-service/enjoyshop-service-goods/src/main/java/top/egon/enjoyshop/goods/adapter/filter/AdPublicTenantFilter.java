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
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import top.egon.cola.component.common.core.pojo.ResultRecord;
import top.egon.enjoyshop.goods.common.enums.GoodsErrorStatus;
import top.egon.enjoyshop.goods.infrastructure.config.AdProperties;

/** Resolves only the anonymous ad-slot route from the servlet server name and restores MDC scope. */
@Component("adPublicTenantFilter")
@Order(Ordered.HIGHEST_PRECEDENCE + 2)
@Slf4j
@RequiredArgsConstructor
public class AdPublicTenantFilter extends OncePerRequestFilter {

    private static final Pattern PUBLIC_AD_ROUTE = Pattern.compile("^/api/v1/ad-slots/[^/]+/ads$");
    private static final Pattern CANONICAL_HOST = Pattern.compile(
            "^(?=.{1,253}$)(?:[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?$");
    private static final String AD_PROPERTIES_BEAN =
            "app.ad-top.egon.enjoyshop.goods.infrastructure.config.AdProperties";

    @Qualifier(AD_PROPERTIES_BEAN)
    private final AdProperties properties;

    @Qualifier("jacksonObjectMapper")
    private final ObjectMapper objectMapper;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        return !"GET".equals(request.getMethod()) || !PUBLIC_AD_ROUTE.matcher(path).matches();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (!properties.isPublicEnabled()) {
            writeSiteNotConfigured(response);
            return;
        }
        String host = canonicalHost(request.getServerName());
        Long tenantId = host == null ? null : properties.getPublicSites().get(host);
        if (tenantId == null || tenantId <= 0) {
            log.debug("anonymous advertisement request has no registered site");
            writeSiteNotConfigured(response);
            return;
        }

        String previousTenantId = MDC.get(TenantContextFilter.TENANT_MDC_KEY);
        MDC.put(TenantContextFilter.TENANT_MDC_KEY, tenantId.toString());
        try {
            chain.doFilter(request, response);
        } finally {
            if (previousTenantId == null) {
                MDC.remove(TenantContextFilter.TENANT_MDC_KEY);
            } else {
                MDC.put(TenantContextFilter.TENANT_MDC_KEY, previousTenantId);
            }
        }
    }

    private static String canonicalHost(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            String host = value.endsWith(".") ? value.substring(0, value.length() - 1) : value;
            String ascii = IDN.toASCII(host, IDN.USE_STD3_ASCII_RULES).toLowerCase(Locale.ROOT);
            return CANONICAL_HOST.matcher(ascii).matches() ? ascii : null;
        } catch (IllegalArgumentException invalidHost) {
            return null;
        }
    }

    private void writeSiteNotConfigured(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader("Cache-Control", "no-store");
        objectMapper.writeValue(response.getOutputStream(), ResultRecord.failure(GoodsErrorStatus.AD_SITE_NOT_CONFIGURED));
    }
}
