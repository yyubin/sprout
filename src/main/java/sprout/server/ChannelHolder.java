package sprout.server;

import java.nio.channels.SelectableChannel;

public final class ChannelHolder {
    private static final ThreadLocal<SelectableChannel> channelHolder = new ThreadLocal<>();

    public static void setChannel(SelectableChannel channel) {
        channelHolder.set(channel);
    }

    public static SelectableChannel getChannel() {
        return channelHolder.get();
    }

    public static void clear() {
        channelHolder.remove();
    }
}
