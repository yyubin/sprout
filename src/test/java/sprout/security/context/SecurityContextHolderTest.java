package sprout.security.context;

import org.junit.jupiter.api.*;
import org.mockito.MockedStatic;
import sprout.config.AppConfig;
import sprout.security.core.SecurityContext;

import java.lang.reflect.Field;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SecurityContextHolderTest {

    @BeforeEach
    void resetHolder() throws Exception {
        Field f = SecurityContextHolder.class.getDeclaredField("strategy");
        f.setAccessible(true);
        f.set(null, null); // static 필드 초기화
    }

    @Test
    @DisplayName("기본 설정(bio) ⇒ ThreadLocalSecurityContextHolderStrategy 선택")
    void defaultStrategy_isThreadLocal() {
        AppConfig cfg = mock(AppConfig.class);
        when(cfg.getStringProperty("sprout.security.strategy", "bio")).thenReturn("bio");

        SecurityContextHolder.initialize(cfg);

        assertTrue(SecurityContextHolder.getContextHolderStrategy() instanceof ThreadLocalSecurityContextHolderStrategy);
    }

    @Test
    @DisplayName("sprout.security.strategy=nio ⇒ ChannelAwareSecurityContextHolderStrategy 선택")
    void nioStrategy_isChannelAware() {
        AppConfig cfg = mock(AppConfig.class);
        when(cfg.getStringProperty("sprout.security.strategy", "bio")).thenReturn("nio");

        // ChannelHolder static 필요 없음: 타입만 확인
        SecurityContextHolder.initialize(cfg);

        assertTrue(SecurityContextHolder.getContextHolderStrategy() instanceof ChannelAwareSecurityContextHolderStrategy);
    }

    @Test
    @DisplayName("initialize는 한 번만 동작하고 이후 호출은 무시된다")
    void initialize_onlyOnce() {
        AppConfig first = mock(AppConfig.class);
        when(first.getStringProperty("sprout.security.strategy", "bio")).thenReturn("bio");
        SecurityContextHolder.initialize(first);

        SecurityContextHolderStrategy s1 = SecurityContextHolder.getContextHolderStrategy();

        AppConfig second = mock(AppConfig.class);
        when(second.getStringProperty("sprout.security.strategy", "bio")).thenReturn("nio");
        SecurityContextHolder.initialize(second);

        assertSame(s1, SecurityContextHolder.getContextHolderStrategy());
        assertTrue(s1 instanceof ThreadLocalSecurityContextHolderStrategy);
    }

    @Test
    @DisplayName("setContext/getContext/clearContext 위임 동작 확인(ThreadLocal 전략)")
    void delegateMethods_threadLocal() {
        AppConfig cfg = mock(AppConfig.class);
        when(cfg.getStringProperty("sprout.security.strategy", "bio")).thenReturn("bio");
        SecurityContextHolder.initialize(cfg);

        SecurityContext ctx1 = SecurityContextHolder.getContext();
        assertNotNull(ctx1);
        assertNull(ctx1.getAuthentication());

        SecurityContext custom = new SecurityContextImpl(null);
        SecurityContextHolder.setContext(custom);
        assertSame(custom, SecurityContextHolder.getContext());

        SecurityContextHolder.clearContext();
        SecurityContext ctx2 = SecurityContextHolder.getContext();
        assertNotSame(custom, ctx2);
    }

    @Test
    @DisplayName("getDeferredContext/setDeferredContext 위임 확인")
    void deferredContext() {
        AppConfig cfg = mock(AppConfig.class);
        when(cfg.getStringProperty("sprout.security.strategy", "bio")).thenReturn("bio");
        SecurityContextHolder.initialize(cfg);

        Supplier<SecurityContext> original = SecurityContextHolder.getDeferredContext();
        assertNotNull(original);
        assertSame(original.get(), SecurityContextHolder.getContext());

        SecurityContext another = new SecurityContextImpl(null);
        Supplier<SecurityContext> sup = () -> another;
        SecurityContextHolder.setDeferredContext(sup);

        assertSame(sup, SecurityContextHolder.getDeferredContext());
        assertSame(another, SecurityContextHolder.getContext());
    }

    @Test
    @DisplayName("createEmptyContext는 strategy의 새 컨텍스트를 반환")
    void createEmpty() {
        AppConfig cfg = mock(AppConfig.class);
        when(cfg.getStringProperty("sprout.security.strategy", "bio")).thenReturn("bio");
        SecurityContextHolder.initialize(cfg);

        SecurityContext empty = SecurityContextHolder.createEmptyContext();
        assertNotNull(empty);
        assertNull(empty.getAuthentication());
    }
}
