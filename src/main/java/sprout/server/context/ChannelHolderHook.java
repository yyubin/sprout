package sprout.server.context;

import sprout.beans.annotation.Component;
import sprout.server.ChannelHolder;

import java.nio.channels.SelectionKey;

//@Component
public class ChannelHolderHook implements ServerRunHook {
    @Override
    public void beforeServerRun(SelectionKey key) throws Exception {
        ChannelHolder.setChannel(key.channel());
    }

    @Override
    public void afterServerRun(SelectionKey key) throws Exception {
        ChannelHolder.clear();
    }
}
