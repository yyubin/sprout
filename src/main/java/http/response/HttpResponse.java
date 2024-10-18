package http.response;

public class HttpResponse<T> {

    private String description;
    private ResponseCode responseCode;
    private T body;

    public HttpResponse(String description, ResponseCode responseCode, T body) {
        this.description = description;
        this.responseCode = responseCode;
        this.body = body;
    }

    public String getDescription() {
        return description;
    }

    public ResponseCode getResponseCode() {
        return responseCode;
    }

    public T getBody() {
        return body;
    }
}
