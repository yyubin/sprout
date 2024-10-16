package request;

import util.Session;

public class HttpRequest<T> {

    private HttpMethod method;
    private String url;
    private T body;
    private Session session;

}
