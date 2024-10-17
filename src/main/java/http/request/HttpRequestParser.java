package http.request;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import exception.BadRequestException;
import http.response.ResponseCode;
import message.ExceptionMessage;

public class HttpRequestParser {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static <T> HttpRequest<?> parse(String rawRequest, Class<T> bodyType) throws IllegalAccessError, JsonProcessingException {
        // GET /example?id=yubin HTTP/1.1

        // POST /example HTTP/1.1
        // {"id": "id", "password": "pwd"} >> json

        String[] requestsLine = rawRequest.split("\n");
        String[] requestParts = requestsLine[0].split(" ");

        if (requestParts.length < 2) {
            throw new BadRequestException(ExceptionMessage.BAD_REQUEST, ResponseCode.BAD_REQUEST);
        }

        String method = requestParts[0].toUpperCase();
        String path = requestParts[1];

        T body = null;

        if ((HttpMethod.GET.getMethod().equals(method)) || (HttpMethod.PUT.getMethod().equals(method))) {
            String jsonBody = requestsLine[1].trim();
            body = objectMapper.readValue(jsonBody, bodyType);
        }

        if ((HttpMethod.DELETE.getMethod().equals(method))) {
            String jsonBody = requestsLine[1].trim();
            if (!jsonBody.isEmpty()) {
                body = objectMapper.readValue(jsonBody, bodyType);
            }
        }

        return new HttpRequest<>(HttpMethod.valueOf(method), path, body);
    }

}
