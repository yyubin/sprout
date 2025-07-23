package sprout.server.builtins;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import sprout.mvc.dispatcher.RequestDispatcher;
import sprout.mvc.http.parser.HttpRequestParser;
import sprout.server.RequestExecutorService;

import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

import static java.nio.channels.SelectionKey.OP_READ;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NioHttpProtocolHandlerTest {

    @Mock RequestDispatcher dispatcher;
    @Mock HttpRequestParser parser;
    @Mock RequestExecutorService executor;
    @Mock SocketChannel channel;
    @Mock Selector selector;
    @Mock SelectionKey selectionKey;

    @Captor ArgumentCaptor<Object> attachmentCaptor;

    @Test
    @DisplayName("accept()는 채널을 OP_READ로 등록하고 HttpConnectionHandler를 attachment로 건다")
    void accept_registers_channel_with_handler() throws Exception {
        // given
        NioHttpProtocolHandler handler =
                new NioHttpProtocolHandler(dispatcher, parser, executor);

        // register 스텁 (final이면 mockito-inline 필요)
        when(channel.register(eq(selector), eq(OP_READ), any()))
                .thenReturn(selectionKey);

        // when
        handler.accept(channel, selector, ByteBuffer.allocate(0));

        // then
        verify(channel).register(eq(selector), eq(OP_READ), attachmentCaptor.capture());
        Object att = attachmentCaptor.getValue();
        assertThat(att).isInstanceOf(HttpConnectionHandler.class);
    }

    @Test
    @DisplayName("supports()는 HTTP/1.1만 true")
    void supports_only_http11() {
        NioHttpProtocolHandler handler =
                new NioHttpProtocolHandler(dispatcher, parser, executor);

        assertThat(handler.supports("HTTP/1.1")).isTrue();
        assertThat(handler.supports("HTTP/2")).isFalse();
        assertThat(handler.supports("WS")).isFalse();
        assertThat(handler.supports(null)).isFalse();
    }
}
