package sprout.server.builtins;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.mockito.stubbing.Answer;
import sprout.mvc.dispatcher.RequestDispatcher;
import sprout.mvc.http.HttpRequest;
import sprout.mvc.http.HttpResponse;
import sprout.mvc.http.ResponseEntity;
import sprout.mvc.http.parser.HttpRequestParser;
import sprout.server.HttpConnectionStatus;
import sprout.server.HttpUtils;
import sprout.server.RequestExecutorService;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

import static java.nio.channels.SelectionKey.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class HttpConnectionHandlerTest {

    @Mock SocketChannel channel;
    @Mock Selector selector;
    @Mock SelectionKey key;
    @Mock RequestDispatcher dispatcher;
    @Mock HttpRequestParser parser;
    @Mock RequestExecutorService executor;

    // interestOps 상태 흉내
    private final AtomicInteger opsHolder = new AtomicInteger();

    HttpConnectionHandler handler;

    @BeforeEach
    void setUp() {
        // key.interestOps 상태 관리
        opsHolder.set(OP_READ);
        when(key.interestOps()).thenAnswer(inv -> opsHolder.get());
        when(key.interestOps(anyInt())).thenAnswer(inv -> {
            opsHolder.set(inv.getArgument(0));
            return key;
        });

        // 기본적으로 read()가 호출될 때 READING 상태여야 하므로 초기 상태 그대로 사용
        // initialBuffer 는 필요 시 각 테스트에서 주입
        handler = new HttpConnectionHandler(channel, selector, dispatcher, parser, executor, /*initial*/ null);
    }

    @Test
    @DisplayName("READ: 완전한 요청이면 PROCESSING → executor 작업 → WRITING 전환 & OP_WRITE 등록")
    void read_completeRequest_submitsTask_and_switchesToWriting() throws Exception {
        String rawReq = "GET / HTTP/1.1\r\nHost: localhost\r\n\r\n";

        // channel.read 가 데이터를 넣도록
        when(channel.read(any(ByteBuffer.class))).thenAnswer(putBytes(rawReq));

        // HttpUtils 정적 모킹
        try (MockedStatic<HttpUtils> ms = mockStatic(HttpUtils.class)) {
            ms.when(() -> HttpUtils.isRequestComplete(any(ByteBuffer.class))).thenReturn(true);
            ms.when(() -> HttpUtils.createResponseBuffer(any()))
                    .thenReturn(ByteBuffer.wrap("HTTP/1.1 200 OK\r\n\r\nOK".getBytes(StandardCharsets.UTF_8)));

            // executor.execute 캡처
            ArgumentCaptor<Runnable> taskCaptor = ArgumentCaptor.forClass(Runnable.class);
            doNothing().when(executor).execute(taskCaptor.capture());

            handler.read(key);

            // PROCESSING 으로 바뀌며 OP 관심을 0으로
            assertThat(opsHolder.get()).isEqualTo(0);

            // 비즈니스 로직 실행
            Runnable task = taskCaptor.getValue();
            // parser/dispatcher 스텁
            when(parser.parse(rawReq)).thenReturn(mock(HttpRequest.class));
            doAnswer(inv -> {
                HttpResponse res = inv.getArgument(1);
                res.setResponseEntity(ResponseEntity.ok("OK"));
                return null;
            }).when(dispatcher).dispatch(any(), any());

            task.run();

            // WRITING 상태가 되었으면 OP_WRITE 세팅
            assertThat(opsHolder.get() & OP_WRITE).isEqualTo(OP_WRITE);
            verify(selector).wakeup();
        }
    }

    @Test
    @DisplayName("WRITE: 전체 전송 완료 시 DONE & close")
    void write_flushAll_then_close() throws Exception {
        prepStateToWriting("HTTP/1.1 200 OK\r\n\r\nhello");

        when(channel.write(any(ByteBuffer.class))).thenAnswer(inv -> {
            ByteBuffer buf = inv.getArgument(0);
            int r = buf.remaining();
            buf.position(buf.limit());
            return r;
        });

        handler.write(key);

        verify(key).cancel();
        verify(channel).close();
    }

    @Test
    @DisplayName("WRITE: 부분 전송이면 남은 데이터, close하지 않음")
    void write_partial_doesNotClose() throws Exception {
        prepStateToWriting("abcde");

        when(channel.write(any(ByteBuffer.class))).thenAnswer(inv -> {
            ByteBuffer buf = inv.getArgument(0);
            int half = Math.max(1, buf.remaining() / 2);
            buf.position(buf.position() + half);
            return half;
        });

        handler.write(key);

        verify(channel, never()).close();
        verify(key, never()).cancel();
    }

    @Test
    @DisplayName("READ: -1 이면 연결 종료")
    void read_remoteClosed_closesChannel() throws Exception {
        when(channel.read(any(ByteBuffer.class))).thenReturn(-1);

        handler.read(key);

        verify(key).cancel();
        verify(channel).close();
    }

    @Test
    @DisplayName("초기 버퍼에 이미 완전한 요청이 들어있어도 처리된다")
    void initialBuffer_alreadyContainsFullRequest() throws Exception {
        // 초기 버퍼에 리퀘스트를 담아서 전달
        ByteBuffer initial = ByteBuffer.wrap("GET / HTTP/1.1\r\nHost:a\r\n\r\n".getBytes(StandardCharsets.UTF_8));
        handler = new HttpConnectionHandler(channel, selector, dispatcher, parser, executor, initial);

        // channel.read 는 0 (추가로 안 읽어도 됨)
        when(channel.read(any(ByteBuffer.class))).thenReturn(0);

        try (MockedStatic<HttpUtils> ms = mockStatic(HttpUtils.class)) {
            ms.when(() -> HttpUtils.isRequestComplete(any(ByteBuffer.class))).thenReturn(true);
            ms.when(() -> HttpUtils.createResponseBuffer(any()))
                    .thenReturn(ByteBuffer.wrap("HTTP/1.1 200 OK\r\n\r\nOK".getBytes(StandardCharsets.UTF_8)));

            ArgumentCaptor<Runnable> taskCaptor = ArgumentCaptor.forClass(Runnable.class);
            doNothing().when(executor).execute(taskCaptor.capture());

            handler.read(key);

            // executor에 제출됨
            Runnable task = taskCaptor.getValue();

            when(parser.parse(anyString())).thenReturn(mock(HttpRequest.class));
            doAnswer(inv -> {
                HttpResponse res = inv.getArgument(1);
                res.setResponseEntity(ResponseEntity.ok("OK"));
                return null;
            }).when(dispatcher).dispatch(any(), any());

            task.run();

            assertThat(opsHolder.get() & OP_WRITE).isEqualTo(OP_WRITE);
            verify(selector).wakeup();
        }
    }

    // ===== Helpers =====

    private void prepStateToWriting(String response) throws Exception {
        Field state = HttpConnectionHandler.class.getDeclaredField("currentState");
        state.setAccessible(true);
        state.set(handler, HttpConnectionStatus.WRITING);

        Field wb = HttpConnectionHandler.class.getDeclaredField("writeBuffer");
        wb.setAccessible(true);
        wb.set(handler, ByteBuffer.wrap(response.getBytes(StandardCharsets.UTF_8)));
    }

    private Answer<Integer> putBytes(String data) {
        byte[] bytes = data.getBytes(StandardCharsets.UTF_8);
        return inv -> {
            ByteBuffer buf = inv.getArgument(0, ByteBuffer.class);
            buf.put(bytes);
            return bytes.length;
        };
    }
}
