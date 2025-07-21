package sprout.security.web;

import sprout.beans.annotation.Component;
import sprout.mvc.dispatcher.DispatchHook;
import sprout.mvc.http.HttpRequest;
import sprout.mvc.http.HttpResponse;
import sprout.security.context.SecurityContextHolder;

@Component
public class SecurityDispatchHook implements DispatchHook {

    @Override
    public void beforeDispatch(HttpRequest<?> request, HttpResponse response) {
        SecurityContextHolder.createEmptyContext();
    }

    @Override
    public void afterDispatch(HttpRequest<?> request, HttpResponse response) {
        SecurityContextHolder.clearContext();
    }
}
