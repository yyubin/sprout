package sprout.mvc.http;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import legacy.config.Container;
import legacy.http.request.ObjectMapperConfig;

public class HttpResponse<T> {

    private ObjectMapper objectMapper = new ObjectMapper();
    private String description;
    private ResponseCode responseCode;
    private String body;

    public HttpResponse(String description, ResponseCode responseCode, T body) throws JsonProcessingException {
        this.description = description;
        this.responseCode = responseCode;
        this.body = objectMapper.writeValueAsString(body);
    }

    public String getDescription() {
        return description;
    }

    public ResponseCode getResponseCode() {
        return responseCode;
    }

    public String getBody() {
        return body;
    }

    @Override
    public String toString() {
        return "HttpResponse{" +
                "description='" + description + '\'' +
                ", responseCode=" + responseCode +
                ", body='" + body + '\'' +
                '}';
    }

    public static <T> HttpResponse<String> ok(T payload) throws JsonProcessingException {
        return new HttpResponse<>("OK", ResponseCode.SUCCESS, payload.toString());
    }

    public static HttpResponse<String> badRequest(String message) throws JsonProcessingException {
        return new HttpResponse<>("Bad Request", ResponseCode.BAD_REQUEST, ResponseCode.BAD_REQUEST.getMessage());
    }

    public static HttpResponse<String> serverError(String message) throws JsonProcessingException {
        return new HttpResponse<>("Server Error", ResponseCode.INTERNAL_SERVER_ERROR, ResponseCode.INTERNAL_SERVER_ERROR.getMessage());
    }

}
