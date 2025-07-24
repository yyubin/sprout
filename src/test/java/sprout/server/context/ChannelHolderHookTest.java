package sprout.server.context;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import sprout.server.ChannelHolder;

import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ChannelHolderHookTest {

    ChannelHolderHook hook = new ChannelHolderHook();

    @AfterEach
    void tearDown() {
        ChannelHolder.clear();
    }

    @Test
    @DisplayName("beforeServerRun() 이 ChannelHolder 에 채널을 넣고, afterServerRun() 이 비운다")
    void beforeAfter() throws Exception {
        SelectionKey key = mock(SelectionKey.class);
        SelectableChannel ch = mock(SelectableChannel.class);
        when(key.channel()).thenReturn(ch);

        hook.beforeServerRun(key);
        assertSame(ch, ChannelHolder.getChannel());

        hook.afterServerRun(key);
        assertNull(ChannelHolder.getChannel());
    }
}
