package sprout.server.context;

import sprout.beans.annotation.Component;
import sprout.beans.annotation.Order;
import sprout.context.ContextPropagator;
import sprout.server.ChannelHolder;

import java.nio.channels.SelectableChannel;

@Component
@Order(30)
public class ChannelContextPropagator implements ContextPropagator<SelectableChannel> {

    @Override
    public void restore(SelectableChannel channel) {
        ChannelHolder.setChannel(channel);
    }

    @Override
    public SelectableChannel capture() {
        return ChannelHolder.getChannel();
    }

    @Override
    public void clear() {
        ChannelHolder.clear();
    }
}
