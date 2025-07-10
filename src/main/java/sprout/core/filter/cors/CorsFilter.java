package sprout.core.filter.cors;

import sprout.beans.annotation.Component;
import sprout.core.filter.Filter;
import sprout.core.filter.FilterChain;
import sprout.mvc.http.HttpRequest;
import sprout.mvc.http.HttpResponse;
import sprout.mvc.http.HttpMethod;

import java.io.IOException;

@Component
public class CorsFilter implements Filter {
    @Override
    public void doFilter(HttpRequest request, HttpResponse response, FilterChain chain) throws IOException {
        response.addHeader("Access-Control-Allow-Origin", "*");
        response.addHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        response.addHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");

        if (request.getMethod().equals(HttpMethod.OPTIONS)) {
            response.addHeader("Access-Control-Max-Age", "3600");
            return;
        }

        chain.doFilter(request, response);
    }
}
