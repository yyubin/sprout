package sprout.server.context;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import sprout.server.ChannelHolder;

import java.nio.channels.SelectableChannel;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ChannelContextPropagatorTest {

    ChannelContextPropagator propagator = new ChannelContextPropagator();

    @AfterEach
    void tearDown() {
        ChannelHolder.clear();
    }

    @Test
    @DisplayName("capture() 는 현재 ChannelHolder 상태를 그대로 반환한다")
    void captureReturnsCurrentChannel() {
        SelectableChannel ch = mock(SelectableChannel.class);
        ChannelHolder.setChannel(ch);

        SelectableChannel captured = propagator.capture();

        assertSame(ch, captured);
    }

    @Test
    @DisplayName("restore() 는 ChannelHolder 에 채널을 세팅한다")
    void restoreSetsChannelHolder() {
        SelectableChannel ch = mock(SelectableChannel.class);
        assertNull(ChannelHolder.getChannel(), "pre-condition: ChannelHolder should be null");

        propagator.restore(ch);

        assertSame(ch, ChannelHolder.getChannel());
    }

    @Test
    @DisplayName("clear() 는 ChannelHolder 를 비운다")
    void clearRemovesChannel() {
        SelectableChannel ch = mock(SelectableChannel.class);
        ChannelHolder.setChannel(ch);

        propagator.clear();

        assertNull(ChannelHolder.getChannel());
    }
}
