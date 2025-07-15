package sprout.mvc.http;

import java.util.HashMap;
import java.util.Map;

public class HttpResponse {
    private ResponseEntity<?> responseEntity;
    private final Map<String, String> headers = new HashMap<>();
    private boolean committed = false;

    public void setResponseEntity(ResponseEntity<?> responseEntity) {
        this.responseEntity = responseEntity;
        this.committed = true;
    }

    public boolean isCommitted() {
        return committed;
    }

    public ResponseEntity<?> getResponseEntity() {
        return responseEntity;
    }

    public void addHeader(String key, String value) {
        this.headers.put(key, value);
    }

    public Map<String, String> getHeaders() {
        return headers;
    }
}