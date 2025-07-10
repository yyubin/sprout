package sprout.core.interceptor;

import sprout.mvc.http.HttpRequest;
import sprout.mvc.http.HttpResponse;

import java.util.List;

public class InterceptorChain {
    private final List<Interceptor> interceptors;

    public InterceptorChain(List<Interceptor> interceptors) {
        this.interceptors = interceptors;
    }

    public boolean applyPreHandle(HttpRequest request, HttpResponse response, Object handler) {
        for (Interceptor interceptor : interceptors) {
            if (!interceptor.preHandle(request, response, handler)) {
                return false;
            }
        }
        return true;
    }

    public void applyPostHandle(HttpRequest request, HttpResponse response, Object handler, Object result) {
        for (int i = interceptors.size() - 1; i >= 0; i--) {
            interceptors.get(i).postHandle(request, response, handler, result);
        }
    }

    public void applyAfterCompletion(HttpRequest request, HttpResponse response, Object handler, Exception ex) {
        for (int i = interceptors.size() - 1; i >= 0; i--) {
            interceptors.get(i).afterCompletion(request, response, handler, ex);
        }
    }
}
