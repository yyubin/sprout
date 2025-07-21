//package sprout.server.builtins;
//
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.params.ParameterizedTest;
//import org.junit.jupiter.params.provider.ValueSource;
//
//import java.io.ByteArrayInputStream;
//import java.io.InputStream;
//import java.nio.charset.StandardCharsets;
//
//import static org.junit.jupiter.api.Assertions.assertEquals;
//class HttpProtocolDetectorTest {
//    private HttpProtocolDetector detector;
//
//    @BeforeEach
//    void setUp() {
//        detector = new HttpProtocolDetector();
//    }
//
//    @ParameterizedTest
//    @ValueSource(strings = {"GET", "POST", "PUT", "DELETE", "HEAD", "OPTIONS", "PATCH", "TRACE"})
//    @DisplayName("다양한 HTTP 메서드를 정확히 HTTP/1.1로 감지해야 한다.")
//    void detect_shouldIdentifyAllHttpMethods(String method) throws Exception {
//        // given
//        String requestLine = String.format("%s /test HTTP/1.1\r\nHost: localhost\r\n\r\n", method);
//        InputStream inputStream = new ByteArrayInputStream(requestLine.getBytes(StandardCharsets.UTF_8));
//
//        // when
//        String protocol = detector.detect(inputStream);
//
//        // then
//        assertEquals("HTTP/1.1", protocol);
//    }
//
//    @Test
//    @DisplayName("HTTP 요청이 아닌 경우 UNKNOWN을 반환해야 한다.")
//    void detect_shouldReturnUnknown_forNonHttpRequest() throws Exception {
//        // given
//        String someData = "This is not an HTTP request.";
//        InputStream inputStream = new ByteArrayInputStream(someData.getBytes(StandardCharsets.UTF_8));
//
//        // when
//        String protocol = detector.detect(inputStream);
//
//        // then
//        assertEquals("UNKNOWN", protocol);
//    }
//
//    @Test
//    @DisplayName("프로토콜 감지 후 스트림은 초기 상태로 리셋되어야 한다.")
//    void detect_shouldResetStream_afterDetection() throws Exception {
//        // given
//        String originalRequest = "POST /api/users HTTP/1.1\r\nContent-Length: 0\r\n\r\n";
//        byte[] originalBytes = originalRequest.getBytes(StandardCharsets.UTF_8);
//        InputStream inputStream = new ByteArrayInputStream(originalBytes);
//
//        // when
//        detector.detect(inputStream); // 감지 실행
//
//        // then
//        // 감지 후에도 스트림을 다시 읽었을 때 원본 데이터가 그대로인지 확인
//        byte[] bytesAfterDetection = inputStream.readAllBytes();
//        assertEquals(originalRequest, new String(bytesAfterDetection, StandardCharsets.UTF_8));
//    }
//}