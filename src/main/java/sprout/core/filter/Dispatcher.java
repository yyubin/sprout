package sprout.core.filter;

import sprout.mvc.http.HttpRequest;
import sprout.mvc.http.HttpResponse;

import java.io.IOException;

@FunctionalInterface
public interface Dispatcher {
    void dispatch(HttpRequest<?> request, HttpResponse response) throws IOException;
}
