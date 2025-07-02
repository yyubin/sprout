package sprout.core.interceptor;

import sprout.mvc.http.HttpRequest;

public interface HandlerInterceptor {
    boolean preHandle(HttpRequest<?> req);
    void postHandle(HttpRequest<?> req, Object returnValue);
}
