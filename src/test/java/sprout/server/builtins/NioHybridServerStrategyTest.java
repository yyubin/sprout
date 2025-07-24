package sprout.server.builtins;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import sprout.server.ConnectionManager;
import sprout.server.ReadableHandler;
import sprout.server.WritableHandler;
import sprout.server.websocket.WebSocketSession;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.nio.channels.*;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NioHybridServerStrategyTest {

    private ConnectionManager connectionManager = mock(ConnectionManager.class);
    private NioHybridServerStrategy strategy = new NioHybridServerStrategy(connectionManager);

    // --------- 공통 유틸 ----------
    private static void setRunning(NioHybridServerStrategy s, boolean v) throws Exception {
        Field f = NioHybridServerStrategy.class.getDeclaredField("running");
        f.setAccessible(true);
        f.setBoolean(s, v);
    }

    private static Selector mockSelectorReturning(SelectionKey... keys) throws Exception {
        Selector sel = mock(Selector.class);
        // selectedKeys()가 immutable이면 remove()에서 터지니 HashSet 사용
        Set<SelectionKey> set = new HashSet<>();
        for (SelectionKey k : keys) set.add(k);
        when(sel.selectedKeys()).thenReturn(set);
        return sel;
    }

    // 한 번 select() 호출되면 루프를 끝내기 위해 running=false로 바꿔줌
    private void stopLoopOnFirstSelect(Selector sel) throws Exception {
        when(sel.select()).thenAnswer(inv -> {
            setRunning(strategy, false);
            return 1;
        });
    }

    @Test
    @DisplayName("OP_ACCEPT 발생 시 ConnectionManager.acceptConnection 호출")
    void accept_event_calls_connectionManager() throws Exception {
        try (MockedStatic<Selector> selStatic = mockStatic(Selector.class);
             MockedStatic<ServerSocketChannel> chStatic = mockStatic(ServerSocketChannel.class)) {

            Selector sel = mockSelectorReturning(mock(SelectionKey.class));
            ServerSocketChannel ssc = mock(ServerSocketChannel.class);

            selStatic.when(Selector::open).thenReturn(sel);
            chStatic.when(ServerSocketChannel::open).thenReturn(ssc);

            SelectionKey key = sel.selectedKeys().iterator().next();
            when(key.isValid()).thenReturn(true);
            when(key.isAcceptable()).thenReturn(true);

            stopLoopOnFirstSelect(sel);

            // port 확인용
            when(ssc.getLocalAddress()).thenReturn(new InetSocketAddress(12345));

            int returned = strategy.start(8080);
            assertEquals(12345, returned);

            // eventLoop 스레드가 한 번 돌도록 잠깐 대기
            Thread.sleep(30);

            verify(connectionManager).acceptConnection(key, sel);
            verify(ssc).bind(new InetSocketAddress(8080));
            verify(ssc).configureBlocking(false);
            verify(ssc).register(sel, SelectionKey.OP_ACCEPT);

            strategy.stop();
        }
    }

    @Test
    @DisplayName("READ 이벤트면 ReadableHandler.read 호출")
    void readable_event_calls_read() throws Exception {
        try (MockedStatic<Selector> selStatic = mockStatic(Selector.class);
             MockedStatic<ServerSocketChannel> chStatic = mockStatic(ServerSocketChannel.class)) {

            Selector sel = mockSelectorReturning(mock(SelectionKey.class));
            ServerSocketChannel ssc = mock(ServerSocketChannel.class);

            selStatic.when(Selector::open).thenReturn(sel);
            chStatic.when(ServerSocketChannel::open).thenReturn(ssc);

            SelectionKey key = sel.selectedKeys().iterator().next();
            ReadableHandler rh = mock(ReadableHandler.class);

            when(key.isValid()).thenReturn(true);
            when(key.isReadable()).thenReturn(true);
            when(key.attachment()).thenReturn(rh);

            stopLoopOnFirstSelect(sel);
            when(ssc.getLocalAddress()).thenReturn(new InetSocketAddress(1));

            strategy.start(0);
            Thread.sleep(30);

            verify(rh).read(key);
            strategy.stop();
        }
    }

    @Test
    @DisplayName("WRITE 이벤트면 WritableHandler.write 호출")
    void writable_event_calls_write() throws Exception {
        try (MockedStatic<Selector> selStatic = mockStatic(Selector.class);
             MockedStatic<ServerSocketChannel> chStatic = mockStatic(ServerSocketChannel.class)) {

            Selector sel = mockSelectorReturning(mock(SelectionKey.class));
            ServerSocketChannel ssc = mock(ServerSocketChannel.class);

            selStatic.when(Selector::open).thenReturn(sel);
            chStatic.when(ServerSocketChannel::open).thenReturn(ssc);

            SelectionKey key = sel.selectedKeys().iterator().next();
            WritableHandler wh = mock(WritableHandler.class);

            when(key.isValid()).thenReturn(true);
            when(key.isWritable()).thenReturn(true);
            when(key.attachment()).thenReturn(wh);

            stopLoopOnFirstSelect(sel);
            when(ssc.getLocalAddress()).thenReturn(new InetSocketAddress(1));

            strategy.start(0);
            Thread.sleep(30);

            verify(wh).write(key);
            strategy.stop();
        }
    }

    @Test
    @DisplayName("핸들러에서 IOException 던지면 cleanupConnection 호출(채널 close & key.cancel)")
    void ioException_triggers_cleanup() throws Exception {
        try (MockedStatic<Selector> selStatic = mockStatic(Selector.class);
             MockedStatic<ServerSocketChannel> chStatic = mockStatic(ServerSocketChannel.class)) {

            Selector sel = mockSelectorReturning(mock(SelectionKey.class));
            ServerSocketChannel ssc = mock(ServerSocketChannel.class);

            selStatic.when(Selector::open).thenReturn(sel);
            chStatic.when(ServerSocketChannel::open).thenReturn(ssc);

            SelectionKey key = sel.selectedKeys().iterator().next();
            ReadableHandler rh = mock(ReadableHandler.class);
            when(key.isValid()).thenReturn(true);
            when(key.isReadable()).thenReturn(true);
            when(key.attachment()).thenReturn(rh);

            // read에서 IOException 던지게
            doThrow(new IOException("boom")).when(rh).read(key);

            // key.channel() 반환용
            SelectableChannel ch = mock(SelectableChannel.class);
            when(key.channel()).thenReturn(ch);
            when(ch.isOpen()).thenReturn(true);

            stopLoopOnFirstSelect(sel);
            when(ssc.getLocalAddress()).thenReturn(new InetSocketAddress(1));

            strategy.start(0);
            Thread.sleep(30);

            verify(key).cancel();
            verify(ch).close();
            strategy.stop();
        }
    }

    @Test
    @DisplayName("cleanUp 시 attachment가 WebSocketSession이면 session.close() 호출")
    void cleanup_closes_websocket_session() throws Exception {
        // private cleanupConnection을 직접 부르기보다, 예외 상황에서 session을 attachment로 넣어 유도
        try (MockedStatic<Selector> selStatic = mockStatic(Selector.class);
             MockedStatic<ServerSocketChannel> chStatic = mockStatic(ServerSocketChannel.class)) {
            Selector sel = mockSelectorReturning(mock(SelectionKey.class));
            ServerSocketChannel ssc = mock(ServerSocketChannel.class);

            SelectableChannel ch = mock(SelectableChannel.class);


            selStatic.when(Selector::open).thenReturn(sel);
            chStatic.when(ServerSocketChannel::open).thenReturn(ssc);

            SelectionKey key = sel.selectedKeys().iterator().next();
            WebSocketSession session = mock(WebSocketSession.class);

            when(key.channel()).thenReturn(ch);
            when(ch.isOpen()).thenReturn(true);

            // read 분기까지 가지 않게 하기 위해 ReadableHandler가 아니므로 아무 일 안 함
            // 대신 writable에서 예외 던져 cleanup 유발
            WritableHandler wh = mock(WritableHandler.class);
            when(key.attachment()).thenReturn(session); // session이지만 instanceof WritableHandler 아님

            // writable에서 캐스팅 안 되니 write는 호출 안되고 cleanup도 안 될 것 → 직접 예외 유발 필요
            var m = NioHybridServerStrategy.class.getDeclaredMethod("cleanupConnection", SelectionKey.class);
            m.setAccessible(true);
            m.invoke(strategy, key);

            verify(session).close();
            verify(key).cancel();
            verify(key.channel()).close();
        }
    }

    @Test
    @DisplayName("stop()은 running=false & selector.wakeup() 호출")
    void stop_sets_flag_and_wakeup() throws Exception {
        Selector sel = mock(Selector.class);
        Field f = NioHybridServerStrategy.class.getDeclaredField("selector");
        f.setAccessible(true);
        f.set(strategy, sel);
        lenient().when(sel.isOpen()).thenReturn(true);

        strategy.stop();

        Field r = NioHybridServerStrategy.class.getDeclaredField("running");
        r.setAccessible(true);
        assertFalse(r.getBoolean(strategy));
        verify(sel).wakeup();
    }

    @Test
    @DisplayName("isRunning()은 플래그와 selector 상태에 의존한다")
    void isRunning_checks_flag_and_selector() throws Exception {
        assertFalse(strategy.isRunning());

        Selector sel = mock(Selector.class);
        Field fs = NioHybridServerStrategy.class.getDeclaredField("selector");
        fs.setAccessible(true);
        fs.set(strategy, sel);

        when(sel.isOpen()).thenReturn(true);
        setRunning(strategy, true);
        assertTrue(strategy.isRunning());

        when(sel.isOpen()).thenReturn(false);
        assertFalse(strategy.isRunning());
    }
}
