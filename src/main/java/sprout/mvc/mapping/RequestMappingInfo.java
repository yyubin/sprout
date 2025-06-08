package sprout.mvc.mapping;

import sprout.mvc.http.HttpMethod;

import java.lang.reflect.Method;
import java.util.Objects;

public class RequestMappingInfo {
    private final String path;
    private final HttpMethod httpMethod;
    private final ControllerInterface controller;
    private final Method handlerMethod;

    public RequestMappingInfo(String path, HttpMethod httpMethod, ControllerInterface controller, Method handlerMethod) {
        this.path = path;
        this.httpMethod = httpMethod;
        this.controller = controller;
        this.handlerMethod = handlerMethod;
    }

    public String getPath() {
        return path;
    }

    public HttpMethod getHttpMethod() {
        return httpMethod;
    }

    public ControllerInterface getController() {
        return controller;
    }

    public Method getHandlerMethod() {
        return handlerMethod;
    }

    // hashCode() 와 equals() 오버라이딩 (맵의 키로 사용될 경우 중요)
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
