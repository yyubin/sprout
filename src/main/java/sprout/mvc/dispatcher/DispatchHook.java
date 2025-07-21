package sprout.mvc.dispatcher;

import sprout.mvc.http.HttpRequest;
import sprout.mvc.http.HttpResponse;

public interface DispatchHook {
    void beforeDispatch(HttpRequest<?> request, HttpResponse response);
    void afterDispatch(HttpRequest<?> request, HttpResponse response);
}
