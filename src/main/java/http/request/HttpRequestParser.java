package http.request;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import exception.BadRequestException;
import http.response.ResponseCode;
import message.ExceptionMessage;

import java.util.HashMap;
import java.util.Map;

public class HttpRequestParser {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static <T> HttpRequest<?> parse(String rawRequest, Class<T> bodyType) throws IllegalAccessError, JsonProcessingException {
        // GET /example?id=yubin HTTP/1.1

        // POST /example HTTP/1.1
        // {"id": "id", "password": "pwd"} >> json

        String[] requestLines = rawRequest.split("\n");
        System.out.println(requestLines.length);
        String[] requestParts = requestLines[0].split(" ");

        if (requestParts.length < 2) {
            throw new BadRequestException(ExceptionMessage.BAD_REQUEST, ResponseCode.BAD_REQUEST);
        }

        String method = requestParts[0].toUpperCase();
        String path = requestParts[1];

        T body = null;
        Map<String, String> queryParams = new HashMap<>();

        String[] pathParts = path.split("\\?");
        path = pathParts[0];
        if (pathParts.length > 1) {
            String queryString = pathParts[1];
            String[] pairs = queryString.split("&");
            for (String pair : pairs) {
                String[] keyValue = pair.split("=");
                if (keyValue.length == 2) {
                    queryParams.put(keyValue[0], keyValue[1]);
                }
            }
        }

        if (HttpMethod.POST.getMethod().equals(method) || HttpMethod.PUT.getMethod().equals(method) || HttpMethod.DELETE.getMethod().equals(method)) {
            if (requestLines.length > 1) {
                String jsonBody = requestLines[1].trim();
                body = objectMapper.readValue(jsonBody, bodyType);
            }
        }

        return new HttpRequest<>(HttpMethod.valueOf(method), path, body, queryParams);
    }

}
