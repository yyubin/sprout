package sprout.core.filter.cors;

import sprout.beans.annotation.Component;
import sprout.config.AppConfig;
import sprout.core.filter.Filter;
import sprout.core.filter.FilterChain;
import sprout.mvc.http.HttpMethod;
import sprout.mvc.http.HttpRequest;
import sprout.mvc.http.HttpResponse;

import java.io.IOException;
import java.util.Optional;

@Component
public class CorsFilter implements Filter {
    private final AppConfig appConfig;

    public CorsFilter(AppConfig appConfig) {
        this.appConfig = appConfig;
    }

    @Override
    public void doFilter(HttpRequest request, HttpResponse response, FilterChain chain) throws IOException {
        String origin = Optional.ofNullable(request.getHeaders().get("Origin"))
                .map(Object::toString)
                .orElse(null);

        if (origin == null || origin.isEmpty()) {
            chain.doFilter(request, response);
            return;
        }

        String allowOrigin = appConfig.getStringProperty("cors.allow-origin", "*");
        boolean allowCredentials = Boolean.parseBoolean(appConfig.getStringProperty("cors.allow-credentials", "false"));
        String allowMethods = appConfig.getStringProperty("cors.allow-methods", "GET, POST, PUT, DELETE, OPTIONS");
        String allowHeaders = appConfig.getStringProperty("cors.allow-headers", "Content-Type, Authorization");
        String exposeHeaders = appConfig.getStringProperty("cors.expose-headers", "");
        String maxAge = appConfig.getStringProperty("cors.max-age", "3600");

        String resolvedOrigin = allowCredentials && "*".equals(allowOrigin) ? origin : allowOrigin;

        response.addHeader("Vary", "Origin");
        response.addHeader("Vary", "Access-Control-Request-Method");
        response.addHeader("Vary", "Access-Control-Request-Headers");
        response.addHeader("Access-Control-Allow-Origin", resolvedOrigin);
        if (allowCredentials) response.addHeader("Access-Control-Allow-Credentials", "true");

        if (request.getMethod().equals(HttpMethod.OPTIONS)) {
            String reqMethod = Optional.ofNullable(request.getHeaders().get("Access-Control-Request-Method"))
                    .map(Object::toString)
                    .orElse(allowMethods);
            String reqHeaders = Optional.ofNullable(request.getHeaders().get("Access-Control-Request-Headers"))
                    .map(Object::toString)
                    .orElse(allowHeaders);

            response.addHeader("Access-Control-Allow-Methods", reqMethod);
            response.addHeader("Access-Control-Allow-Headers", reqHeaders);
            response.addHeader("Access-Control-Max-Age", maxAge);
            response.addHeader("Content-Length", "0");
            return;
        }

        response.addHeader("Access-Control-Allow-Methods", allowMethods);
        response.addHeader("Access-Control-Allow-Headers", allowHeaders);
        if (!exposeHeaders.isBlank()) response.addHeader("Access-Control-Expose-Headers", exposeHeaders);

        chain.doFilter(request, response);
    }

}
