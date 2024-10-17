package http.request;

import util.Session;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HttpRequest<T> {

    private HttpMethod method;
    private String path;
    private T body;
    private Map<String, String> queryParams;

    public HttpRequest(HttpMethod method, String path, T body, Map<String, String> queryParams) {
        this.method = method;
        this.path = path;
        this.body = body;
        this.queryParams = queryParams;
    }

    public HttpMethod getMethod() {
        return method;
    }

    public String getPath() {
        return path;
    }

    public T getBody() {
        return body;
    }

    public Map<String, String> getQueryParams() {
        return queryParams;
    }

}
