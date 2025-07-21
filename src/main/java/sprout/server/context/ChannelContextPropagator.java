package sprout.server.context;

import sprout.beans.annotation.Component;
import sprout.beans.annotation.Order;
import sprout.context.ContextPropagator;
import sprout.server.ChannelHolder;

import java.nio.channels.SelectableChannel;

@Component
@Order(30)
public class ChannelContextPropagator implements ContextPropagator {
    private SelectableChannel channel;

    @Override
    public void capture() {
        channel = ChannelHolder.getChannel();
    }

    @Override
    public void restore() {
        ChannelHolder.setChannel(channel);
    }

    @Override
    public void clear() {
        ChannelHolder.clear();
    }
}
