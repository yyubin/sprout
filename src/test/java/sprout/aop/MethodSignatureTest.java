package sprout.aop;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;

class MethodSignatureTest {

    static class Sample {
        public String hi(int a, String b) { return ""; }
        void noArg() {}
    }

    private static Method m(String name, Class<?>... types) {
        try { return Sample.class.getDeclaredMethod(name, types); }
        catch (NoSuchMethodException e) { throw new AssertionError(e); }
    }

    @Test
    @DisplayName("기본 정보 getter 확인")
    void getters() {
        Method method = m("hi", int.class, String.class);
        MethodSignature sig = new MethodSignature(method);

        assertEquals("hi", sig.getName());
        assertEquals(String.class, sig.getReturnType());
        assertArrayEquals(new Class<?>[]{int.class, String.class}, sig.getParameterTypes());
        assertEquals(Sample.class, sig.getDeclaringType());
    }

    @Test
    @DisplayName("toString() 포맷 및 캐싱 (동일 객체 반환)")
    void toString_cached() {
        MethodSignature sig = new MethodSignature(m("hi", int.class, String.class));

        String first = sig.toString();
        String second = sig.toString();   // 캐시 사용
        assertSame(first, second, "캐시된 String 인스턴스를 재사용해야 한다");

        assertEquals("String Sample.hi(int, String)", first);
    }

    @Test
    @DisplayName("toLongName() 캐싱 확인 (double-checked locking)")
    void toLongName_cached() {
        Method method = m("hi", int.class, String.class);
        MethodSignature sig = new MethodSignature(method);

        String first = sig.toLongName();
        String second = sig.toLongName();
        assertSame(first, second);
        assertEquals(method.toGenericString(), first);
    }

    @Test
    @DisplayName("동시 다발 호출에도 한 번만 계산되어 동일 객체 반환 (thread-safe)")
    void threadSafety_onCaching() throws InterruptedException {
        MethodSignature sig = new MethodSignature(m("noArg"));
        int threads = 20;

        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done  = new CountDownLatch(threads);

        Set<String> toStringRefs = Collections.newSetFromMap(new ConcurrentHashMap<>());
        Set<String> longNameRefs = Collections.newSetFromMap(new ConcurrentHashMap<>());

        for (int i = 0; i < threads; i++) {
            new Thread(() -> {
                try {
                    start.await();
                    toStringRefs.add(sig.toString());
                    longNameRefs.add(sig.toLongName());
                } catch (InterruptedException ignored) {
                } finally {
                    done.countDown();
                }
            }).start();
        }

        start.countDown();
        done.await();

        assertEquals(1, toStringRefs.size(), "toString() 결과가 하나여야 함(캐싱)");
        assertEquals(1, longNameRefs.size(), "toLongName() 결과가 하나여야 함(캐싱)");
    }
}
