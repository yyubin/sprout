package sprout.context;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class DummyPropagator implements ContextPropagator<String> {
    private final ThreadLocal<String> tl = new ThreadLocal<>();

    @Override
    public String capture() {
        return tl.get();
    }

    @Override
    public void restore(String value) {
        tl.set(value);
    }

    @Override
    public void clear() {
        tl.remove();
    }

    void set(String v) { tl.set(v); }
    String get() { return tl.get(); }
}

class ContextSnapshotTest {

    @Test
    @DisplayName("Runnable 래핑: restore 후 실행되고 finally에서 clear 호출")
    void wrapRunnable_restoreAndClear() {
        DummyPropagator p1 = new DummyPropagator();
        DummyPropagator p2 = new DummyPropagator();

        p1.set("A");
        p2.set("B");

        ContextSnapshot snap = new ContextSnapshot(List.of(p1, p2));

        // 실행 전 값 바꿔서 스냅샷 복구 여부 확인
        p1.set("X");
        p2.set(null);

        AtomicReference<String> seen = new AtomicReference<>();
        Runnable task = () -> seen.set(p1.get() + p2.get());

        Runnable wrapped = snap.wrap(task);
        wrapped.run();

        assertEquals("AB", seen.get(), "restore로 캡쳐 시점의 값이 적용되어야 한다");
        assertNull(p1.get(), "clear() 후 값이 없어야 한다");
        assertNull(p2.get(), "clear() 후 값이 없어야 한다");
    }

    @Test
    @DisplayName("Callable 래핑: 결과 반환 및 clear 호출 보장")
    void wrapCallable_restoreAndClear() throws Exception {
        DummyPropagator p = new DummyPropagator();
        p.set("CTX");

        ContextSnapshot snap = new ContextSnapshot(List.of(p));

        p.set("OTHER");

        Callable<String> callable = () -> "val:" + p.get();
        Callable<String> wrapped = snap.wrap(callable);

        String result = wrapped.call();
        assertEquals("val:CTX", result);
        assertNull(p.get());
    }

    @Test
    @DisplayName("예외 발생해도 clear()는 반드시 실행된다")
    void clearCalledOnException() {
        DummyPropagator p = new DummyPropagator();
        p.set("CTX");

        ContextSnapshot snap = new ContextSnapshot(List.of(p));

        RuntimeException boom = new RuntimeException("boom");
        Runnable r = () -> { throw boom; };

        Runnable wrapped = snap.wrap(r);
        RuntimeException ex = assertThrows(RuntimeException.class, wrapped::run);
        assertSame(boom, ex);
        assertNull(p.get(), "예외 이후에도 clear 되어야 한다");
    }

    @Test
    @DisplayName("빈 propagator 리스트면 원래 Runnable/Callable 그대로 반환")
    void emptyPropagators_returnsSameInstance() throws Exception {
        ContextSnapshot snap = new ContextSnapshot(List.of());

        Runnable r = () -> {};
        Callable<String> c = () -> "ok";

        assertSame(r, snap.wrap(r));
        assertSame(c, snap.wrap(c));
    }

    @Test
    @DisplayName("여러 propagator 각각 올바르게 복구/정리된다")
    void multiplePropagators() throws Exception {
        DummyPropagator p1 = new DummyPropagator();
        DummyPropagator p2 = new DummyPropagator();
        p1.set("1");
        p2.set("2");

        ContextSnapshot snap = new ContextSnapshot(List.of(p1, p2));

        // 변경
        p1.set(null);
        p2.set("X");

        Callable<String> c = () -> p1.get() + ":" + p2.get();
        String res = snap.wrap(c).call();

        assertEquals("1:2", res);
        assertNull(p1.get());
        assertNull(p2.get());
    }
}
