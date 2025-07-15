package sprout.mvc.http;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ResponseEntity<T>{
    private final ResponseCode statusCode;
    private final Map<String, String> headers;
    private final T body;
    private String contentType = "application/json";

    public ResponseEntity(T body, Map<String, String> headers, ResponseCode statusCode) {
        this.body = body;
        this.headers = headers;
        this.statusCode = statusCode;
    }

    public ResponseEntity(ResponseCode statusCode, T body) {
        this.statusCode = statusCode;
        this.body = body;
        this.headers = Collections.emptyMap();
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public void addHeader(String key, String value) {
        headers.put(key, value);
    }

    public ResponseCode getStatusCode() {
        return statusCode;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public T getBody() {
        return body;
    }

    public static <T> ResponseEntity<T> ok(T body) {
        return new ResponseEntity<>(body, new HashMap<>(), ResponseCode.SUCCESS);
    }

    public static <T> ResponseEntity<T> created(T body) {
        return new ResponseEntity<>(body, new HashMap<>(), ResponseCode.CREATED);
    }

    public static ResponseEntity<Void> noContent() {
        return new ResponseEntity<>(null, new HashMap<>(), ResponseCode.NO_CONTENT);
    }

    public static ResponseEntity<Void> badRequest() {
        return new ResponseEntity<>(null, new HashMap<>(), ResponseCode.BAD_REQUEST);
    }

    public static ResponseEntity<Void> notFound() {
        return new ResponseEntity<>(null, new HashMap<>(), ResponseCode.NOT_FOUND);
    }

}
