package sprout.security.context;

import sprout.security.core.SecurityContext;
import sprout.server.ChannelHolder;

import java.nio.channels.SelectableChannel;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class ChannelAwareSecurityContextHolderStrategy implements SecurityContextHolderStrategy{

    private final Map<SelectableChannel, SecurityContext> contextMap = new ConcurrentHashMap<>();

    @Override
    public void clearContext() {
        SelectableChannel channel = ChannelHolder.getChannel();
        if (channel != null) {
            contextMap.remove(channel);
        }
    }

    @Override
    public SecurityContext getContext() {
        SelectableChannel channel = ChannelHolder.getChannel();
        if (channel == null) {
            return createEmptyContext();
        }
        // 맵에 없으면 새로 생성하여 넣어줌
        return contextMap.computeIfAbsent(channel, k -> createEmptyContext());
    }

    @Override
    public Supplier<SecurityContext> getDeferredContext() {
        return SecurityContextHolderStrategy.super.getDeferredContext();
    }

    @Override
    public void setContext(SecurityContext context) {
        SelectableChannel channel = ChannelHolder.getChannel();
        if (channel != null) {
            contextMap.put(channel, context);
        }
    }

    @Override
    public void setDeferredContext(Supplier<SecurityContext> deferredContext) {
        SecurityContextHolderStrategy.super.setDeferredContext(deferredContext);
    }

    @Override
    public SecurityContext createEmptyContext() {
        return new SecurityContextImpl(null);
    }
}
