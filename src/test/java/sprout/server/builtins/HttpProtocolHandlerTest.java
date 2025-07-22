package sprout.server.builtins;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sprout.mvc.dispatcher.RequestDispatcher;
import sprout.mvc.http.HttpMethod;
import sprout.mvc.http.HttpRequest;
import sprout.mvc.http.HttpResponse;
import sprout.mvc.http.ResponseEntity;
import sprout.mvc.http.parser.HttpRequestParser;
import sprout.server.ThreadService;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HttpProtocolHandlerTest {

    @Mock
    private ThreadService mockThreadService;
    @Mock
    private RequestDispatcher mockDispatcher;
    @Mock
    private HttpRequestParser mockParser;
    @Mock
    private SocketChannel mockChannel;
    @Mock
    private Socket mockSocket;

    @InjectMocks
    private HttpProtocolHandler httpProtocolHandler;

    @BeforeEach
    void setUp() throws Exception {
        // channel.socket()이 mockSocket을 반환하도록 설정
        lenient().when(mockChannel.socket()).thenReturn(mockSocket);

        // ThreadService가 Runnable을 받으면 즉시 현재 스레드에서 실행하도록 설정
        lenient().doAnswer(invocation -> {
            Runnable task = invocation.getArgument(0);
            task.run();
            return null;
        }).when(mockThreadService).execute(any(Runnable.class));
    }

    @Test
    @DisplayName("supports 메서드는 'HTTP/1.1' 프로토콜을 지원해야 한다")
    void supports_should_return_true_for_http_protocol() {
        assertTrue(httpProtocolHandler.supports("HTTP/1.1"));
        assertFalse(httpProtocolHandler.supports("WEBSOCKET"));
    }

    @Test
    @DisplayName("accept 메서드가 유효한 HTTP 요청을 성공적으로 처리해야 한다")
    void accept_should_process_valid_http_request() throws Exception {
        // given: 요청/응답 데이터 준비
        String requestPart1 = "GET /test HTTP/1.1\r\n";
        String requestPart2 = "Host: example.com\r\n\r\n";
        String fullRequest = requestPart1 + requestPart2;

        ByteBuffer initialBuffer = ByteBuffer.wrap(requestPart1.getBytes(StandardCharsets.UTF_8));
        InputStream inputStream = new ByteArrayInputStream(requestPart2.getBytes(StandardCharsets.UTF_8));
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream(); // 응답을 캡처할 스트림

        // given: I/O 스트림 Mock 설정
        when(mockSocket.getInputStream()).thenReturn(inputStream);
        when(mockSocket.getOutputStream()).thenReturn(outputStream);

        // given: 파서 및 디스패처 Mock 설정
        HttpRequest<?> mockRequest = new HttpRequest<>(HttpMethod.GET, "/test", null, new HashMap<>(), new HashMap<>()); // 더미 요청 객체
        doReturn(mockRequest).when(mockParser).parse(fullRequest);

        // dispatcher.dispatch가 호출되면, 응답 객체(res)에 값을 채워넣도록 설정
        doAnswer(invocation -> {
            HttpResponse res = invocation.getArgument(1);
            res.setResponseEntity(ResponseEntity.ok("Success"));
            return null;
        }).when(mockDispatcher).dispatch(eq(mockRequest), any(HttpResponse.class));

        // when: 핸들러 실행
        httpProtocolHandler.accept(mockChannel, null, initialBuffer);

        // then: 상호작용 검증
        verify(mockParser).parse(fullRequest);
        verify(mockDispatcher).dispatch(eq(mockRequest), any(HttpResponse.class));

        // then: OutputStream에 쓰인 응답 결과 검증
        String expectedResponse = "HTTP/1.1 200 OK\r\n" +
                "Content-Type: application/json\r\n" +
                "Content-Length: 7\r\n" +
                "\r\n" +
                "Success";
        assertEquals(expectedResponse, outputStream.toString(StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("accept 메서드가 빈 요청을 받으면 아무 작업도 하지 않아야 한다")
    void accept_should_do_nothing_for_blank_request() throws Exception {
        // given
        ByteBuffer initialBuffer = ByteBuffer.wrap("\r\n".getBytes(StandardCharsets.UTF_8));
        InputStream inputStream = new ByteArrayInputStream(new byte[0]);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        when(mockSocket.getInputStream()).thenReturn(inputStream);
        when(mockSocket.getOutputStream()).thenReturn(outputStream);

        // when
        httpProtocolHandler.accept(mockChannel, null, initialBuffer);

        // then
        // 파서나 디스패처가 호출되지 않았는지 확인
        verifyNoInteractions(mockParser, mockDispatcher);
        // 응답이 쓰이지 않았는지 확인
        assertEquals(0, outputStream.size());
    }
}