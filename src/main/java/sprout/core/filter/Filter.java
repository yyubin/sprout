package sprout.core.filter;

import sprout.beans.InfrastructureBean;
import sprout.mvc.http.HttpRequest;
import sprout.mvc.http.HttpResponse;

import java.io.IOException;

public interface Filter extends InfrastructureBean {
    void doFilter(HttpRequest request, HttpResponse response, FilterChain chain) throws IOException;
}