package sprout.mvc.http;


import legacy.config.Container;
import legacy.http.request.ObjectMapperConfig;

public class HttpResponse<T> {

    private String description;
    private ResponseCode responseCode;
    private String body;

    public HttpResponse(String description, ResponseCode responseCode, T body) {
        this.description = description;
        this.responseCode = responseCode;
        this.body = Container.getInstance().get(ObjectMapperConfig.class).toJson(body);
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

    public static <T> HttpResponse<String> ok(T payload) {
        String json = sprout.context.Container.getInstance().get(ObjectMapperConfig.class).toJson(payload);
        return new HttpResponse<>("OK", ResponseCode.SUCCESS, json);
    }

    public static HttpResponse<String> badRequest(String message) {
        String json = sprout.context.Container.getInstance().get(ObjectMapperConfig.class).toJson(message);
        return new HttpResponse<>("Bad Request", ResponseCode.BAD_REQUEST, json);
    }

    public static HttpResponse<String> serverError(String message) {
        String json = sprout.context.Container.getInstance().get(ObjectMapperConfig.class).toJson(message);
        return new HttpResponse<>("Server Error", ResponseCode.INTERNAL_SERVER_ERROR, json);
    }

}
