//package legacy.request;
//
//import app.exception.BadRequestException;
//import legacy.http.request.HttpRequestParser;
//import legacy.http.request.ObjectMapperConfig;
//import sprout.mvc.http.HttpMethod;
//import sprout.mvc.http.HttpRequest;
//import sprout.mvc.http.ResponseCode;
//import app.message.ExceptionMessage;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//
//import java.util.Map;
//
//import static org.junit.jupiter.api.Assertions.*;
//
//public class HttpRequestParserTests {
//
//    private HttpRequestParser httpRequestParser;
//
//    @BeforeEach
//    void setup() {
//        httpRequestParser = new HttpRequestParser(new ObjectMapperConfig());
//    }
//
//    @Test
//    void testParseValidPostRequest() throws Exception {
//        String rawRequest = "POST /posts/add?boardId=1\n" +
//                "{\"postName\":\"Test Post\", \"postContent\":\"This is a test.\"}";
//
//        HttpRequest<Map<String, Object>> request = httpRequestParser.parse(rawRequest);
//
//        assertEquals(HttpMethod.POST, request.getMethod());
//        assertEquals("/posts/add", request.getPath());
//        assertEquals("1", request.getQueryParams().get("boardId"));
//    }
//
//    @Test
//    void testParseValidGetRequest() throws Exception {
//        String rawRequest = "GET /posts/view?postId=1&boardId=2\n";
//
//        HttpRequest<Map<String, Object>> request = httpRequestParser.parse(rawRequest);
//
//        assertEquals(HttpMethod.GET, request.getMethod());
//        assertEquals("/posts/view", request.getPath());
//        assertEquals("1", request.getQueryParams().get("postId"));
//        assertEquals("2", request.getQueryParams().get("boardId"));
//        assertNull(request.getBody());
//    }
//
//    @Test
//    void testParseBadRequest() {
//        String rawRequest = "BADREQUEST";
//
//        Exception exception = assertThrows(BadRequestException.class, () -> {
//            httpRequestParser.parse(rawRequest);
//        });
//
//        assertEquals(ResponseCode.BAD_REQUEST + " " + ExceptionMessage.BAD_REQUEST, exception.getMessage());
//    }
//
//
//}
