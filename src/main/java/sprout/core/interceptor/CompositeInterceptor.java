package sprout.core.interceptor;

import sprout.mvc.http.HttpRequest;

import java.util.List;

public class CompositeInterceptor {
    private final List<HandlerInterceptor> delegates;

    public CompositeInterceptor(List<HandlerInterceptor> delegates) {
        this.delegates = delegates;
    }

    public boolean preHandle(HttpRequest<?> r){
        for (HandlerInterceptor i : delegates)
            if (!i.preHandle(r)) return false;
        return true;
    }
    public void postHandle(HttpRequest<?> r, Object v){
        for (int idx=delegates.size()-1; idx>=0; idx--)
            delegates.get(idx).postHandle(r, v);
    }
}
