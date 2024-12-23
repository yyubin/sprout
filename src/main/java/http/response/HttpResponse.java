package http.response;

import com.fasterxml.jackson.databind.ObjectMapper;
import config.Container;
import http.request.ObjectMapperConfig;

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
}
