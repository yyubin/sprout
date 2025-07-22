package sprout.security.context;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import sprout.security.core.Authentication;
import sprout.security.core.SecurityContext;
import sprout.server.ChannelHolder;

import java.nio.channels.SelectableChannel;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ChannelAwareSecurityContextHolderStrategyTest {

    @Test
    @DisplayName("채널이 없으면 비어있는 SecurityContext 를 반환하고 맵에 저장하지 않는다")
    void getContext_noChannel_returnsEmpty() {
        ChannelAwareSecurityContextHolderStrategy strategy = new ChannelAwareSecurityContextHolderStrategy();

        try (MockedStatic<ChannelHolder> holder = mockStatic(ChannelHolder.class)) {
            holder.when(ChannelHolder::getChannel).thenReturn(null);

            SecurityContext ctx = strategy.getContext();
            assertNotNull(ctx);
            assertNull(ctx.getAuthentication());

            // clearContext() 호출해도 NPE 없어야 함
            assertDoesNotThrow(strategy::clearContext);
        }
    }

    @Test
    @DisplayName("채널별로 Context 가 분리된다 (채널1/채널2 서로 다른 인스턴스)")
    void perChannelIsolation() {
        ChannelAwareSecurityContextHolderStrategy strategy = new ChannelAwareSecurityContextHolderStrategy();
        SelectableChannel ch1 = mock(SelectableChannel.class);
        SelectableChannel ch2 = mock(SelectableChannel.class);

        try (MockedStatic<ChannelHolder> holder = mockStatic(ChannelHolder.class)) {
            // ch1
            holder.when(ChannelHolder::getChannel).thenReturn(ch1);
            SecurityContext c1a = strategy.getContext();
            SecurityContext c1b = strategy.getContext();
            assertSame(c1a, c1b);

            // ch2
            holder.when(ChannelHolder::getChannel).thenReturn(ch2);
            SecurityContext c2 = strategy.getContext();
            assertNotSame(c1a, c2);
        }
    }

    @Test
    @DisplayName("setContext는 현재 채널에 매핑된다")
    void setContext_putsIntoMap() {
        ChannelAwareSecurityContextHolderStrategy strategy = new ChannelAwareSecurityContextHolderStrategy();
        SelectableChannel ch = mock(SelectableChannel.class);
        Authentication auth = mock(Authentication.class);
        SecurityContext newCtx = new SecurityContextImpl(auth);

        try (MockedStatic<ChannelHolder> holder = mockStatic(ChannelHolder.class)) {
            holder.when(ChannelHolder::getChannel).thenReturn(ch);

            strategy.setContext(newCtx);
            SecurityContext fetched = strategy.getContext();

            assertSame(newCtx, fetched);
            assertSame(auth, fetched.getAuthentication());
        }
    }

    @Test
    @DisplayName("clearContext는 현재 채널의 Context 만 제거한다")
    void clearContext_removesOnlyCurrentChannel() {
        ChannelAwareSecurityContextHolderStrategy strategy = new ChannelAwareSecurityContextHolderStrategy();
        SelectableChannel ch1 = mock(SelectableChannel.class);
        SelectableChannel ch2 = mock(SelectableChannel.class);

        SecurityContext ctx1 = new SecurityContextImpl(mock(Authentication.class));
        SecurityContext ctx2 = new SecurityContextImpl(mock(Authentication.class));

        try (MockedStatic<ChannelHolder> holder = mockStatic(ChannelHolder.class)) {
            // put ch1
            holder.when(ChannelHolder::getChannel).thenReturn(ch1);
            strategy.setContext(ctx1);

            // put ch2
            holder.when(ChannelHolder::getChannel).thenReturn(ch2);
            strategy.setContext(ctx2);

            // clear ch1
            holder.when(ChannelHolder::getChannel).thenReturn(ch1);
            strategy.clearContext();
            assertNull(strategy.getContext().getAuthentication()); // 새 빈 컨텍스트 생성됨

            // ch2는 그대로
            holder.when(ChannelHolder::getChannel).thenReturn(ch2);
            assertSame(ctx2, strategy.getContext());
        }
    }
}
