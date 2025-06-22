package sprout.mvc.mapping;

import sprout.mvc.http.HttpMethod;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Objects;

public record RequestMappingInfo<T>(PathPattern pattern, HttpMethod httpMethod, Object controller, Method handlerMethod, Map<String, String> pathVariables) {

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RequestMappingInfo that = (RequestMappingInfo) o;
        return Objects.equals(pattern, that.pattern) &&
                httpMethod == that.httpMethod;
    }

    @Override
    public int hashCode() {
        return Objects.hash(pattern.getOriginalPattern(), httpMethod);
    }
}
