package sprout.server.builtins;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import sprout.server.ConnectionManager;
import sprout.server.context.ServerRunHook;

import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NioHybridServerStrategyTest {
    @Mock
    private ConnectionManager connectionManager;

    @Mock
    private Selector mockSelector; // 가짜 Selector

    @InjectMocks
    private NioHybridServerStrategy serverStrategy;

    @Test
    @DisplayName("OP_ACCEPT 이벤트가 발생하면 connectionManager.acceptConnection을 호출한다")
    void should_handle_accept_event() throws Exception {
        try (MockedStatic<Selector> mockSelectorStatic = Mockito.mockStatic(Selector.class);
             MockedStatic<ServerSocketChannel> mockServerChannelStatic = Mockito.mockStatic(ServerSocketChannel.class)) {

            // given - 가짜 객체들 준비
            Selector mockSelector = mock(Selector.class);
            ServerSocketChannel mockServerChannel = mock(ServerSocketChannel.class);
            SelectionKey mockKey = mock(SelectionKey.class);

            // 이전 UnsupportedOperationException 해결
            Set<SelectionKey> mockKeys = new HashSet<>(Set.of(mockKey));

            // 정적 메서드 호출 시 가짜 객체를 반환하도록 설정
            mockSelectorStatic.when(Selector::open).thenReturn(mockSelector);
            mockServerChannelStatic.when(ServerSocketChannel::open).thenReturn(mockServerChannel);

            // 무한 루프를 탈출하기 위한 트릭
            when(mockSelector.select()).thenAnswer(invocation -> {
                Field runningField = NioHybridServerStrategy.class.getDeclaredField("running");
                runningField.setAccessible(true);
                if ((boolean) runningField.get(serverStrategy)) {
                    runningField.set(serverStrategy, false);
                    return 1;
                }
                return 0;
            });

            when(mockSelector.selectedKeys()).thenReturn(mockKeys);
            when(mockKey.isAcceptable()).thenReturn(true);

            // when
            serverStrategy.start(8080);

            // then
            verify(connectionManager).acceptConnection(mockKey, mockSelector);
            verify(mockServerChannel).bind(new InetSocketAddress(8080));
            verify(mockServerChannel).configureBlocking(false);
            verify(mockServerChannel).register(mockSelector, SelectionKey.OP_ACCEPT);
        }
    }

    @Test
    @DisplayName("stop 메서드가 호출되면 running 상태를 false로 바꾸고 selector를 wakeup 시킨다")
    void stop_should_set_running_to_false_and_wakeup_selector() throws Exception {
        // given
        Field selectorField = NioHybridServerStrategy.class.getDeclaredField("selector");
        selectorField.setAccessible(true);
        selectorField.set(serverStrategy, mockSelector);
        when(mockSelector.isOpen()).thenReturn(true);

        // when
        serverStrategy.stop();

        // then
        Field runningField = NioHybridServerStrategy.class.getDeclaredField("running");
        runningField.setAccessible(true);
        boolean running = (boolean) runningField.get(serverStrategy);

        assertFalse(running); // running 상태가 false가 되었는지 확인
        verify(mockSelector).wakeup(); // selector.wakeup()이 호출되었는지 확인
    }
}