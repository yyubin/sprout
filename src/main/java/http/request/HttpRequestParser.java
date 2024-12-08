package http.request;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import config.annotations.Component;
import config.annotations.Priority;
import config.annotations.Requires;
import exception.BadRequestException;
import http.response.ResponseCode;
import message.ExceptionMessage;

import java.util.HashMap;
import java.util.Map;


@Component
@Priority(value = 1)
@Requires(dependsOn = {ObjectMapperConfig.class})
public class HttpRequestParser {

    private final ObjectMapperConfig objectMapper;

    public HttpRequestParser(ObjectMapperConfig objectMapper) {
        this.objectMapper = objectMapper;
    }

    private String[] splitRequest(String rawRequest) {
        int headerEndIndex = rawRequest.indexOf("\r\n\r\n");
        System.out.println(headerEndIndex);
        if (headerEndIndex == -1) {
            headerEndIndex = rawRequest.indexOf("\n\n");
        }

        if (headerEndIndex != -1) {
            String requestLineAndHeaders = rawRequest.substring(0, headerEndIndex);
            String bodyPart = rawRequest.substring(headerEndIndex + (rawRequest.charAt(headerEndIndex) == '\r' ? 4 : 2));
            return new String[]{requestLineAndHeaders, bodyPart};
        } else {
            return new String[]{rawRequest, ""};
        }
    }

    public HttpRequest<Map<String, Object>> parse(String rawRequest) throws Exception {

        String[] requestSections = splitRequest(rawRequest);
        String requestLineAndHeaders = requestSections[0];
        String bodyPart = requestSections[1];

        String[] requestLines = requestLineAndHeaders.split("\r\n");
        if (requestLines.length < 1) {
            throw new BadRequestException(ExceptionMessage.BAD_REQUEST, ResponseCode.BAD_REQUEST);
        }

        String requestLine = requestLines[0];
        String[] requestParts = requestLine.split(" ");
        if (requestParts.length < 3) {
            throw new BadRequestException(ExceptionMessage.BAD_REQUEST, ResponseCode.BAD_REQUEST);
        }

        String method = requestParts[0].toUpperCase();
        String path = requestParts[1];

        String[] pathParts = path.split("\\?");
        String cleanPath = pathParts[0];
        Map<String, String> queryParams = new HashMap<>();
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

        Map<String, Object> body = null;
        if (!bodyPart.isEmpty()) {
            body = parseBodyToMap(bodyPart.trim());
        }

        return new HttpRequest<>(
                HttpMethod.valueOf(method),
                cleanPath,
                body,
                queryParams
        );
    }

    private Map<String, String> parseQueryParams(String[] pathParts) {
        Map<String, String> queryParams = new HashMap<>();
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
        return queryParams;
    }

    private boolean requiresBody(HttpMethod method) {
        return method == HttpMethod.POST || method == HttpMethod.PUT || method == HttpMethod.DELETE;
    }

    private Map<String, Object> parseBodyToMap(String body) throws Exception {
        if (body.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.getObjectMapper().readValue(body, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            throw new BadRequestException("Failed to parse JSON body", e);
        }
    }
}
