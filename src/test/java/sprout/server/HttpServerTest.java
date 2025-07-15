package sprout.server;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import sprout.mvc.dispatcher.RequestDispatcher;
import sprout.mvc.http.parser.HttpRequestParser;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertThrows;

class HttpServerTest {

    private HttpServer httpServer;

    @Mock
    private ServerStrategy mockServerStrategy;

    @Mock
    private RequestDispatcher mockDispatcher;

    @Mock
    private ThreadService mockThreadService;

    @Mock
    private HttpRequestParser mockRequestParser;

    private ExecutorService testPool;

    private int testPort;
    private Thread serverThread;

    private void startServer(int port) {
        serverThread = new Thread(() -> {
            try {
                httpServer.start(port);
            } catch (Exception ignored) {
            }
        });
        serverThread.start();
    }

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        testPool = Executors.newFixedThreadPool(1);

        httpServer = new HttpServer(mockServerStrategy);

        try (ServerSocket s = new ServerSocket(0)) {
            testPort = s.getLocalPort();
        }
    }

    @AfterEach
    void tearDown() throws Exception {
        if (serverThread != null && serverThread.isAlive()) {
            serverThread.interrupt();
            serverThread.join(2000);
        }

        if (testPool != null) {
            testPool.shutdownNow();
            testPool.awaitTermination(1, TimeUnit.SECONDS);
        }

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
    }

    @Test
    @DisplayName("이미 사용 중인 포트면 BindException")
    void start_portAlreadyInUse_throwsBindException() throws Exception {

        int busyPort;
        try (ServerSocket probe = new ServerSocket(0)) {
            busyPort = probe.getLocalPort();
        }

        try (ServerSocket conflict = new ServerSocket(busyPort)) {

            HttpServer another = new HttpServer(mockServerStrategy);

            assertThrows(java.net.BindException.class,
                    () -> another.start(busyPort));
        }
    }
}