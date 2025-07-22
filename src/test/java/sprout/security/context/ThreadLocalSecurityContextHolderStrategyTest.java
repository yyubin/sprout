package sprout.security.context;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import sprout.security.core.SecurityContext;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

class ThreadLocalSecurityContextHolderStrategyTest {

    ThreadLocalSecurityContextHolderStrategy strategy = new ThreadLocalSecurityContextHolderStrategy();

    @Test
    @DisplayName("getContext()는 최초 호출 시 빈 컨텍스트를 생성하고 이후엔 같은 인스턴스를 반환한다")
    void getContext_cachedEmpty() {
        SecurityContext ctx1 = strategy.getContext();
        SecurityContext ctx2 = strategy.getContext();
        assertNotNull(ctx1);
        assertSame(ctx1, ctx2);
        assertNull(ctx1.getAuthentication());
    }

    @Test
    @DisplayName("setContext()로 설정한 컨텍스트를 그대로 돌려준다")
    void setContext_returnsSame() {
        SecurityContext custom = new SecurityContextImpl(null);
        strategy.setContext(custom);

        assertSame(custom, strategy.getContext());
    }

    @Test
    @DisplayName("setDeferredContext(Supplier)로 지연 설정된다")
    void setDeferredContext_supplier() {
        SecurityContext ctx = new SecurityContextImpl(null);
        Supplier<SecurityContext> supplier = () -> ctx;

        strategy.setDeferredContext(supplier);

        // getDeferredContext()는 동일 supplier를 돌려줘야 함
        assertSame(supplier, strategy.getDeferredContext());
        assertSame(ctx, strategy.getContext());
    }

    @Test
    @DisplayName("clearContext() 후에는 새 빈 컨텍스트가 다시 생성된다")
    void clearContext_createsNew() {
        SecurityContext before = strategy.getContext();
        strategy.clearContext();
        SecurityContext after = strategy.getContext();

        assertNotSame(before, after);
        assertNull(after.getAuthentication());
    }

    @Test
    @DisplayName("ThreadLocal이므로 다른 스레드와 컨텍스트가 분리된다")
    void threadIsolation() throws ExecutionException, InterruptedException {
        SecurityContext mainCtx = strategy.getContext();

        Callable<SecurityContext> task = () -> {
            // 다른 스레드에서 처음 호출 -> 새 컨텍스트
            return strategy.getContext();
        };

        FutureTask<SecurityContext> ft = new FutureTask<>(task);
        Thread t = new Thread(ft);
        t.start();
        SecurityContext otherThreadCtx = ft.get();

        assertNotSame(mainCtx, otherThreadCtx);
    }
}
