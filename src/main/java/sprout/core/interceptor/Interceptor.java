package sprout.core.interceptor;

import sprout.beans.InfrastructureBean;
import sprout.mvc.http.HttpRequest;
import sprout.mvc.http.HttpResponse;

public interface Interceptor extends InfrastructureBean {
    boolean preHandle(HttpRequest request, HttpResponse response, Object handler);

    void postHandle(HttpRequest request, HttpResponse response, Object handler, Object result);

    void afterCompletion(HttpRequest request, HttpResponse response, Object handler, Exception ex);
}
