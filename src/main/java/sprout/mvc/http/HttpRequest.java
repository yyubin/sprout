package sprout.mvc.http;

import app.util.Session;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

public class HttpRequest<T> {

    private HttpMethod method;
    private String path;
    private T body;
    private Map<String, String> queryParams;
    private Map<String, String> headers;
    private String sessionId;

    public HttpRequest(HttpMethod method, String path, T body, Map<String, String> queryParams, Map<String, String> headers) {
        this.method = method;
        this.path = path;
        this.body = body;
        this.queryParams = Collections.unmodifiableMap(Objects.requireNonNull(queryParams, "Query parameters cannot be null"));;
        this.headers = Collections.unmodifiableMap(Objects.requireNonNull(headers, "Headers cannot be null"));;
        this.sessionId = Session.getSessionId();
    }

    public Map<String, String> getHeaders() {
        return headers;
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

    public String getSessionId() {
        return sessionId;
    }

    @Override
    public String toString() {
        return "HttpRequest{" +
                "method=" + method +
                ", path='" + path + '\'' +
                ", body=" + body +
                ", queryParams=" + queryParams +
                ", sessionId='" + sessionId + '\'' +
                '}';
    }

    public boolean isValid() {
        return this.method != null && !this.path.isBlank();
    }
}
