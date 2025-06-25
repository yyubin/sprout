package sprout.server;

import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import sprout.mvc.dispatcher.RequestDispatcher;
import sprout.mvc.http.HttpResponse;
import sprout.mvc.http.ResponseCode;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class ConnectionHandlerTest {

    @Mock
    RequestDispatcher dispatcher;

    private AutoCloseable mocks;
    private ExecutorService pool;

    @BeforeEach
    void setUp() {
        mocks  = MockitoAnnotations.openMocks(this);
        pool   = Executors.newSingleThreadExecutor();   // Handler를 돌릴 쓰레드
    }

    @AfterEach
    void tearDown() throws Exception {
        pool.shutdownNow();
        mocks.close();
    }

    @Test
    @DisplayName("요청을 읽어 dispatcher 에 전달하고 응답을 클라이언트로 쓴다")
    void handle_request_and_write_response() throws Exception {

        String body = "{\"msg\":\"ok\"}";
        HttpResponse<String> mockResp = new HttpResponse<>(ResponseCode.SUCCESS.getMessage(), ResponseCode.SUCCESS, body);
        when(dispatcher.dispatch(anyString())).thenReturn(mockResp);

        try (ServerSocket server = new ServerSocket(0)) {          // 임시 포트
            int port = server.getLocalPort();

            // 클라이언트 소켓(테스트 스레드)
            Socket client = new Socket("localhost", port);

            // 서버 측 소켓(Handler 용)
            Socket serverSide = server.accept();

            // 핸들러 구동
            ConnectionHandler handler = new ConnectionHandler(serverSide, dispatcher);
            pool.submit(handler);           // 백그라운드 실행

            // 요청 전송
            String req = "GET /hello HTTP/1.1\r\n" +
                    "Host: localhost\r\n" +
                    "Connection: close\r\n\r\n";
            client.getOutputStream().write(req.getBytes(StandardCharsets.UTF_8));
            client.getOutputStream().flush();
            client.shutdownOutput();        // EOF 신호

            // 응답 읽기
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(client.getInputStream(), StandardCharsets.UTF_8));

            String statusLine = reader.readLine();          // HTTP/1.1 200 OK
            assertThat(statusLine).isEqualTo("HTTP/1.1 200 OK");

            // 헤더 스킵 후 본문
            String line;
            while (!(line = reader.readLine()).isEmpty()) {}   // 빈 줄까지 소비
            int contentLen = body.getBytes(StandardCharsets.UTF_8).length;
            char[] buf = new char[contentLen];
            int read = 0;
            while (read < contentLen) {
                int n = reader.read(buf, read, contentLen - read);
                if (n == -1) break;          // 예외 상황
                read += n;
            }
            assertThat(new String(buf)).isEqualTo(body);

            // dispatcher 호출 검증
            verify(dispatcher, timeout(500).times(1)).dispatch(anyString());

            client.close();
        }
    }

    // POST + body : Content-Length 를 읽고 dispatcher 로 원문이 전달되는지 확인
    @Test
    @DisplayName("Content-Length 가 있는 요청도 완전히 읽는다")
    void handle_post_with_body() throws Exception {

        String postBody = "name=yubin";
        HttpResponse<String> resp = new HttpResponse<>(ResponseCode.SUCCESS.getMessage(), ResponseCode.SUCCESS, "done");
        when(dispatcher.dispatch(anyString())).thenReturn(resp);

        try (ServerSocket server = new ServerSocket(0)) {
            int port = server.getLocalPort();
            Socket client = new Socket("localhost", port);
            Socket serverSide = server.accept();

            pool.submit(new ConnectionHandler(serverSide, dispatcher));

            String req = "POST /submit HTTP/1.1\r\n" +
                    "Host: localhost\r\n" +
                    "Content-Type: application/x-www-form-urlencoded\r\n" +
                    "Content-Length: " + postBody.getBytes().length + "\r\n" +
                    "Connection: close\r\n\r\n" +
                    postBody;
            client.getOutputStream().write(req.getBytes(StandardCharsets.UTF_8));
            client.getOutputStream().flush();
            client.shutdownOutput();

            // dispatcher 로 전달된 원문에 body 가 포함됐는지 캡처
            ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
            verify(dispatcher, timeout(500)).dispatch(captor.capture());
            assertThat(captor.getValue()).contains(postBody);

            client.close();
        }
    }
}
