package sprout.server.builtins;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sprout.server.AcceptableProtocolHandler;
import sprout.server.ByteBufferPool;
import sprout.server.ProtocolDetector;

import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DefaultConnectionManagerTest {

    @Mock
    private ProtocolDetector mockDetector;
    @Mock
    private AcceptableProtocolHandler mockHandler; // AcceptableProtocolHandler로 Mocking

    @Mock
    private SelectionKey mockSelectionKey;
    @Mock
    private Selector mockSelector;
    @Mock
    private ServerSocketChannel mockServerChannel;
    @Mock
    private SocketChannel mockClientChannel;
    @Mock
    private ByteBufferPool mockByteBufferPool;

    private DefaultConnectionManager connectionManager;

    @BeforeEach
    void setUp() {
        ByteBuffer mockBuffer = ByteBuffer.allocate(8192);
        when(mockByteBufferPool.acquire(anyInt())).thenReturn(mockBuffer);

        connectionManager = new DefaultConnectionManager(List.of(mockDetector), List.of(mockHandler), mockByteBufferPool);
    }

    @Test
    @DisplayName("프로토콜 감지 및 처리가 성공적으로 수행되어야 한다")
    void should_detect_and_handle_protocol_successfully() throws Exception {
        // given (준비)
        String detectedProtocol = "HTTP";
        ByteBuffer buffer = ByteBuffer.allocate(2048);

        // 채널 관련 Mock 동작 설정
        when(mockSelectionKey.channel()).thenReturn(mockServerChannel);
        when(mockServerChannel.accept()).thenReturn(mockClientChannel);
        // read()가 호출되면 128 바이트를 읽었다고 가정
        when(mockClientChannel.read(any(ByteBuffer.class))).thenReturn(128);

        // Detector와 Handler Mock 동작 설정
        when(mockDetector.detect(any(ByteBuffer.class))).thenReturn(detectedProtocol);
        when(mockHandler.supports(detectedProtocol)).thenReturn(true);

        // when (실행)
        connectionManager.acceptConnection(mockSelectionKey, mockSelector);

        // then (검증)
        verify(mockDetector).detect(any(ByteBuffer.class));
        verify(mockHandler).supports(detectedProtocol);
        // mockHandler의 accept 메서드가 정확한 인자들로 호출되었는지 검증
        verify(mockHandler).accept(eq(mockClientChannel), eq(mockSelector), any(ByteBuffer.class));
        // 채널이 닫히지 않았는지 확인
        verify(mockClientChannel, never()).close();
    }

    @Test
    @DisplayName("프로토콜을 감지하지 못하면 연결을 닫아야 한다")
    void should_close_connection_when_protocol_is_unknown() throws Exception {
        // given
        when(mockSelectionKey.channel()).thenReturn(mockServerChannel);
        when(mockServerChannel.accept()).thenReturn(mockClientChannel);
        when(mockClientChannel.read(any(ByteBuffer.class))).thenReturn(128);

        // Detector가 "UNKNOWN"을 반환하도록 설정
        when(mockDetector.detect(any(ByteBuffer.class))).thenReturn("UNKNOWN");

        // when
        connectionManager.acceptConnection(mockSelectionKey, mockSelector);

        // then
        // 핸들러의 어떤 메서드도 호출되지 않았는지 확인
        verify(mockHandler, never()).supports(anyString());
        verify(mockHandler, never()).accept(any(), any(), any());
        // 채널이 닫혔는지 확인
        verify(mockClientChannel).close();
    }

    @Test
    @DisplayName("채널에서 데이터를 읽지 못하면 연결을 즉시 닫아야 한다")
    void should_close_connection_when_no_bytes_are_read() throws Exception {
        // given
        when(mockSelectionKey.channel()).thenReturn(mockServerChannel);
        when(mockServerChannel.accept()).thenReturn(mockClientChannel);
        // read()가 -1(연결 종료)을 반환하도록 설정
        when(mockClientChannel.read(any(ByteBuffer.class))).thenReturn(-1);

        // when
        connectionManager.acceptConnection(mockSelectionKey, mockSelector);

        // then
        // 채널이 닫혔는지 확인
        verify(mockClientChannel).close();
        // Detector나 Handler는 전혀 호출되면 안 됨
        verifyNoInteractions(mockDetector, mockHandler);
    }
}