package sprout.mvc.http;

import app.util.Session;

import java.util.Map;

public class HttpRequest<T> {

    private HttpMethod method;
    private String path;
    private T body;
    private Map<String, String> queryParams;
    private String sessionId;

    public HttpRequest(HttpMethod method, String path, T body, Map<String, String> queryParams) {
        this.method = method;
        this.path = path;
        this.body = body;
        this.queryParams = queryParams;
        this.sessionId = Session.getSessionId();
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
}
