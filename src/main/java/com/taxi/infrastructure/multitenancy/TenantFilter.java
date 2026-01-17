package com.taxi.infrastructure.multitenancy;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
public class TenantFilter extends OncePerRequestFilter {

    private static final String TENANT_HEADER = "X-Tenant-ID";
    private static final String TENANT_PARAM = "tenantId";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        try {
            String tenantId = resolveTenant(request);

            if (tenantId == null || tenantId.isBlank()) {
                throw new ServletException(
                    "Missing tenant for request: " + request.getRequestURI()
                );
            }

            tenantId = sanitizeTenantId(tenantId);
            TenantContext.setCurrentTenant(tenantId);

            log.debug("Tenant [{}] bound to {}", tenantId, request.getRequestURI());

            filterChain.doFilter(request, response);

        } finally {
            TenantContext.clear();
        }
    }

    private String resolveTenant(HttpServletRequest request) {
        // 1. Header
        String tenantId = request.getHeader(TENANT_HEADER);
        if (tenantId != null && !tenantId.isBlank()) {
            return tenantId;
        }

        // 2. Query param (optional; consider removing long-term)
        tenantId = request.getParameter(TENANT_PARAM);
        if (tenantId != null && !tenantId.isBlank()) {
            return tenantId;
        }

        // 3. Subdomain
        String host = request.getServerName();
        if (host != null && host.contains(".")) {
            String subdomain = host.split("\\.")[0];
            if (isValidSubdomain(subdomain)) {
                return subdomain;
            }
        }

        return null;
    }

    private boolean isValidSubdomain(String subdomain) {
        return !subdomain.equalsIgnoreCase("www")
            && !subdomain.equalsIgnoreCase("api")
            && !subdomain.equalsIgnoreCase("localhost")
            && !subdomain.matches("\\d+");
    }

    private String sanitizeTenantId(String tenantId) {
        tenantId = tenantId.toLowerCase().trim();
        tenantId = tenantId.replaceAll("[^a-z0-9_]", "");

        if (tenantId.length() > 63) {
            tenantId = tenantId.substring(0, 63);
        }

        return tenantId;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // ONLY actuator should bypass tenant logic
        return request.getRequestURI().startsWith("/actuator");
    }
}
