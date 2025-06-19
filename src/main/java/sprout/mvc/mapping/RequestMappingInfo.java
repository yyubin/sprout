package sprout.mvc.mapping;

import sprout.mvc.http.HttpMethod;

import java.lang.reflect.Method;
import java.util.Objects;

public record RequestMappingInfo<T>(String path, HttpMethod httpMethod, Object controller, Method handlerMethod) {

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RequestMappingInfo that = (RequestMappingInfo) o;
        return Objects.equals(path, that.path) &&
                httpMethod == that.httpMethod;
    }

    @Override
    public int hashCode() {
        return Objects.hash(path, httpMethod);
    }
}
