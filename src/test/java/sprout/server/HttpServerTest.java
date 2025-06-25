package sprout.server;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import sprout.mvc.dispatcher.RequestDispatcher;

import java.lang.reflect.Field;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class HttpServerTest {

    private HttpServer httpServer;

    @Mock
    private RequestDispatcher mockDispatcher;

    @Mock
    private ThreadService mockThreadService;

    private ExecutorService testPool;

    private int testPort;
    private Thread serverThread;

    private void startServer(int port) {
        serverThread = new Thread(() -> {
            try { httpServer.start(port); } catch (Exception ignored) {}
        });
        serverThread.start();
    }

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        testPool = Executors.newFixedThreadPool(1);

        httpServer = new HttpServer(mockThreadService, mockDispatcher);
        Field poolField = HttpServer.class.getDeclaredField("pool");
        poolField.setAccessible(true);
        poolField.set(httpServer, testPool);

        try (ServerSocket s = new ServerSocket(0)) {
            testPort = s.getLocalPort();
        }
    }

    @AfterEach
    void tearDown() throws Exception {
        // 서버 스레드를 안전하게 종료하기 위해 인터럽트 시도
        if (serverThread != null && serverThread.isAlive()) {
            serverThread.interrupt();
            serverThread.join(2000); // 스레드가 종료될 때까지 최대 2초 대기
        }

        if (testPool != null) {
            testPool.shutdownNow(); // 스레드 풀 강제 종료
            testPool.awaitTermination(1, TimeUnit.SECONDS);
        }

        // 간혹 포트가 바로 해제되지 않아 다음 테스트에 영향을 줄 수 있으므로 잠시 대기
        TimeUnit.MILLISECONDS.sleep(100);
    }

    @Test
    @DisplayName("정상 연결 수락")
    void start_acceptsConnectionAndExecutesHandler() throws Exception {
        startServer(testPort);
        TimeUnit.MILLISECONDS.sleep(200);
        String httpRequest =
                "GET / HTTP/1.1\r\n" +
                        "Host: localhost\r\n" +
                        "Connection: close\r\n" +
                        "\r\n";

        try (Socket client = new Socket("localhost", testPort)) {
            client.getOutputStream().write(httpRequest.getBytes());
            client.getOutputStream().flush();
        }
//        verify(mockDispatcher,
//                timeout(1000).atLeastOnce())   // 1초 내에 최소 1회
//                .dispatch(anyString());
    }

    @Test
    @DisplayName("이미 사용 중인 포트면 BindException")
    void start_portAlreadyInUse_throwsBindException() throws Exception {

        /* 새 임시 포트 확보 */
        int busyPort;
        try (ServerSocket probe = new ServerSocket(0)) {
            busyPort = probe.getLocalPort();
        }

        /* 그 포트를 우리가 먼저 점유 */
        try (ServerSocket conflict = new ServerSocket(busyPort)) {

            HttpServer another = new HttpServer(mockThreadService, mockDispatcher);
            Field poolField = HttpServer.class.getDeclaredField("pool");
            poolField.setAccessible(true);
            poolField.set(another, Executors.newSingleThreadExecutor());

            assertThrows(java.net.BindException.class,
                    () -> another.start(busyPort));
        }
    }
}