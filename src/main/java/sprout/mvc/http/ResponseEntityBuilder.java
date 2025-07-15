package sprout.mvc.http;

import java.util.HashMap;
import java.util.Map;

public class ResponseEntityBuilder {
    private Object body;
    private ResponseCode status = ResponseCode.SUCCESS;
    private final Map<String, String> headers = new HashMap<>();
    private String contentType = "application/json";


    public static ResponseEntityBuilder status(ResponseCode status) {
        ResponseEntityBuilder builder = new ResponseEntityBuilder();
        builder.status = status;
        return builder;
    }

    public static ResponseEntityBuilder ok() {
        return status(ResponseCode.SUCCESS);
    }

    public static ResponseEntityBuilder created() {
        return status(ResponseCode.CREATED);
    }

    public static ResponseEntityBuilder noContent() {
        return status(ResponseCode.NO_CONTENT);
    }

    public ResponseEntityBuilder body(Object body) {
        this.body = body;
        return this;
    }

    public ResponseEntityBuilder contentType(String contentType) {
        this.contentType = contentType;
        return this;
    }

    public ResponseEntityBuilder header(String key, String value) {
        this.headers.put(key, value);
        return this;
    }

    public ResponseEntity<?> build() {
        ResponseEntity<Object> res = new ResponseEntity<>(status, body);
        res.setContentType(contentType);
        headers.forEach(res::addHeader);
        return res;
    }

}
