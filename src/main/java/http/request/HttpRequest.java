package http.request;

import util.Session;

public class HttpRequest<T> {

    private HttpMethod method;
    private String path;
    private T body;

    public HttpRequest(HttpMethod method, String path, T body) {
        this.method = method;
        this.path = path;
        this.body = body;
    }
}
