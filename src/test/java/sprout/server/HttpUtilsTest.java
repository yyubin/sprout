package sprout.server;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import sprout.mvc.http.ResponseCode;
import sprout.mvc.http.ResponseEntity;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class HttpUtilsTest {

    // ---------- Helpers ----------
    private static ByteBuffer bb(String s) {
        ByteBuffer b = ByteBuffer.allocate(1024);
        b.put(s.getBytes(StandardCharsets.UTF_8));
        b.flip();
        return b;
    }

    private static String asString(ByteBuffer buf) {
        byte[] arr = new byte[buf.remaining()];
        buf.get(arr);
        return new String(arr, StandardCharsets.UTF_8);
    }

    // ---------- isRequestComplete ----------
    @Nested
    @DisplayName("isRequestComplete()")
    class IsRequestCompleteTests {

        @Test
        @DisplayName("null 또는 비어있는 버퍼 -> false")
        void nullOrEmpty() {
            assertThat(HttpUtils.isRequestComplete(null)).isFalse();

            ByteBuffer empty = ByteBuffer.allocate(10);
            empty.flip(); // remaining 0
            assertThat(HttpUtils.isRequestComplete(empty)).isFalse();
        }

        @Test
        @DisplayName("GET 요청(바디 없음) -> 헤더만 완성되면 true")
        void getRequest_noBody() {
            String req = "GET / HTTP/1.1\r\nHost: x\r\n\r\n";
            assertThat(HttpUtils.isRequestComplete(bb(req))).isTrue();
        }

        @Test
        @DisplayName("Content-Length: 5 인 경우 - 바디 부족 -> false, 충분 -> true")
        void contentLength_bodyCompleteCheck() {
            String header = "POST / HTTP/1.1\r\nHost: x\r\nContent-Length: 5\r\n\r\n";
            String body = "12345";
            String bodyShort = "12";

            // 부족
            assertThat(HttpUtils.isRequestComplete(bb(header + bodyShort))).isFalse();
            // 충분
            assertThat(HttpUtils.isRequestComplete(bb(header + body))).isTrue();
        }

        @Test
        @DisplayName("chunked: 완성되지 않음 -> false, 완성 -> true")
        void chunked_check() {
            String header = "POST / HTTP/1.1\r\nHost: x\r\nTransfer-Encoding: chunked\r\n\r\n";
            String incomplete =
                    "3\r\nabc\r\n" +
                            "2\r\nxy";                // 마지막 CRLF/사이즈 부족
            String complete =
                    "3\r\nabc\r\n" +
                            "2\r\nxy\r\n" +
                            "0\r\n\r\n";

            assertThat(HttpUtils.isRequestComplete(bb(header + incomplete))).isFalse();
            assertThat(HttpUtils.isRequestComplete(bb(header + complete))).isTrue();
        }
    }

    // ---------- readRawRequest ----------
    @Nested
    @DisplayName("readRawRequest()")
    class ReadRawRequestTests {

        @Test
        @DisplayName("Content-Length가 있는 요청을 끝까지 읽는다")
        void read_contentLength() throws Exception {
            String header = "POST /foo HTTP/1.1\r\nHost: a\r\nContent-Length: 4\r\n\r\n";
            String body = "ABCD";
            String all = header + body;

            ByteBuffer initial = bb(""); // 초기엔 아무것도 없음
            InputStream in = new ByteArrayInputStream(all.getBytes(StandardCharsets.UTF_8));

            String raw = HttpUtils.readRawRequest(initial, in);

            assertThat(raw).isEqualTo(all);
        }

        @Test
        @DisplayName("chunked 요청을 끝까지 읽는다 (단순 케이스)")
        void read_chunked() throws Exception {
            String request =
                    "POST /c HTTP/1.1\r\nHost: a\r\nTransfer-Encoding: chunked\r\n\r\n" +
                            "4\r\nWiki\r\n" +
                            "5\r\npedia\r\n" +
                            "0\r\n\r\n"; // 끝

            String expectedBody = "Wikipedia"; // HttpUtils.readChunkedBody가 그대로 붙임

            ByteBuffer initial = bb(""); // nothing initially
            InputStream in = new ByteArrayInputStream(request.getBytes(StandardCharsets.UTF_8));

            String raw = HttpUtils.readRawRequest(initial, in);

            // 헤더/CRLF/바디 확인
            assertThat(raw).contains("Transfer-Encoding: chunked");
            assertThat(raw).endsWith(expectedBody);
        }
    }

    // ---------- createResponseBuffer ----------
    @Nested
    @DisplayName("createResponseBuffer()")
    class CreateResponseBufferTests {

        @Test
        @DisplayName("null ResponseEntity -> null 반환")
        void nullEntity() {
            assertThat(HttpUtils.createResponseBuffer(null)).isNull();
        }

        @Test
        @DisplayName("정상 ResponseEntity -> Status line/Headers/Body 포함")
        void normalResponse() {
            ResponseEntity res = new ResponseEntity(
                    "Hello",
                    Map.of("X-Test", "1"),
                    ResponseCode.SUCCESS,
                    "text/plain"
            );

            ByteBuffer buf = HttpUtils.createResponseBuffer(res);
            String out = asString(buf);

            assertThat(out).startsWith("HTTP/1.1 200 OK\r\n");
            assertThat(out).contains("Content-Type: text/plain");
            assertThat(out).contains("Content-Length: 5");
            assertThat(out).contains("X-Test: 1");
            assertThat(out).endsWith("Hello");
        }
    }

}
