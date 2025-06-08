package sprout.mvc.mapping;

import sprout.mvc.http.HttpMethod;
import sprout.mvc.invoke.HandlerMethod;

public interface HandlerMapping {
    HandlerMethod findHandler(String path, HttpMethod httpMethod);
}
