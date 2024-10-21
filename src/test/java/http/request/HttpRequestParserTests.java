package http.request;

import com.fasterxml.jackson.core.JsonProcessingException;
import exception.BadRequestException;
import http.response.ResponseCode;
import message.ExceptionMessage;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class HttpRequestParserTests {

    @Test
    void testParseValidPostRequest() throws JsonProcessingException {
        String rawRequest = "POST /posts/add?boardId=1\n" +
                "{\"postName\":\"Test Post\", \"postContent\":\"This is a test.\"}";

        HttpRequest<Map<String, Object>> request = HttpRequestParser.parse(rawRequest);

        assertEquals(HttpMethod.POST, request.getMethod());
        assertEquals("/posts/add", request.getPath());
        assertEquals("1", request.getQueryParams().get("boardId"));
        assertEquals("Test Post", request.getBody().get("postName"));
        assertEquals("This is a test.", request.getBody().get("postContent"));
    }

    @Test
    void testParseValidGetRequest() throws JsonProcessingException {
        String rawRequest = "GET /posts/view?postId=1&boardId=2\n";

        HttpRequest<Map<String, Object>> request = HttpRequestParser.parse(rawRequest);

        assertEquals(HttpMethod.GET, request.getMethod());
        assertEquals("/posts/view", request.getPath());
        assertEquals("1", request.getQueryParams().get("postId"));
        assertEquals("2", request.getQueryParams().get("boardId"));
        assertNull(request.getBody());
    }

    @Test
    void testParseBadRequest() {
        String rawRequest = "BADREQUEST";

        Exception exception = assertThrows(BadRequestException.class, () -> {
            HttpRequestParser.parse(rawRequest);
        });

        assertEquals(ResponseCode.BAD_REQUEST + " " + ExceptionMessage.BAD_REQUEST, exception.getMessage());
    }

    @Test
    void testParseInvalidJsonBody() {
        String rawRequest = "POST /posts/add\n" +
                "Invalid JSON";

        Exception exception = assertThrows(JsonProcessingException.class, () -> {
            HttpRequestParser.parse(rawRequest);
        });

        assertNotNull(exception);
    }


}
