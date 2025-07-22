package sprout.aop.advisor;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AdvisorRegistryTest {

    // 간단한 더미 타겟
    static class Target {
        void foo() {}
        void bar() {}
    }

    private Method m(String name) throws NoSuchMethodException {
        return Target.class.getDeclaredMethod(name);
    }

    @Test
    @DisplayName("registerAdvisor 시 order 기준 정렬된다")
    void register_sortsByOrder() {
        AdvisorRegistry reg = new AdvisorRegistry();

        Advisor high = mock(Advisor.class);
        when(high.getOrder()).thenReturn(10);

        Advisor low = mock(Advisor.class);
        when(low.getOrder()).thenReturn(1);

        reg.registerAdvisor(high);
        reg.registerAdvisor(low);

        List<Advisor> all = reg.getAllAdvisors();
        assertEquals(List.of(low, high), all);
    }

    @Test
    @DisplayName("getApplicableAdvisors: pointcut 매칭되는 것만 반환 & order 정렬")
    void applicable_filtersAndSorts() throws Exception {
        AdvisorRegistry reg = new AdvisorRegistry();

        Pointcut pcTrue = mock(Pointcut.class);
        Pointcut pcFalse = mock(Pointcut.class);
        when(pcTrue.matches(any(), any())).thenReturn(true);
        when(pcFalse.matches(any(), any())).thenReturn(false);

        Advisor a1 = mock(Advisor.class);
        when(a1.getPointcut()).thenReturn(pcTrue);
        when(a1.getOrder()).thenReturn(5);

        Advisor a2 = mock(Advisor.class);
        when(a2.getPointcut()).thenReturn(pcFalse);
        when(a2.getOrder()).thenReturn(1);

        Advisor a3 = mock(Advisor.class);
        when(a3.getPointcut()).thenReturn(pcTrue);
        when(a3.getOrder()).thenReturn(2);

        reg.registerAdvisor(a1);
        reg.registerAdvisor(a2);
        reg.registerAdvisor(a3);

        List<Advisor> list = reg.getApplicableAdvisors(Target.class, m("foo"));
        // a2는 false 이므로 제외, a3(order2) -> a1(order5) 순
        assertEquals(List.of(a3, a1), list);
    }

    @Test
    @DisplayName("캐시 사용: 한 번 계산 후 cachedAdvisors에 저장, 새 advisor 등록 시 캐시 초기화")
    @SuppressWarnings("unchecked")
    void cache_isStoredAndCleared() throws Exception {
        AdvisorRegistry reg = new AdvisorRegistry();

        Pointcut pc = mock(Pointcut.class);
        when(pc.matches(any(), any())).thenReturn(true);

        Advisor a1 = mock(Advisor.class);
        when(a1.getPointcut()).thenReturn(pc);
        when(a1.getOrder()).thenReturn(1);

        reg.registerAdvisor(a1);

        // 첫 조회 -> 캐시 생성
        reg.getApplicableAdvisors(Target.class, m("foo"));

        Field f = AdvisorRegistry.class.getDeclaredField("cachedAdvisors");
        f.setAccessible(true);
        Map<Class<?>, List<Advisor>> cache = (Map<Class<?>, List<Advisor>>) f.get(reg);
        assertTrue(cache.containsKey(Target.class));

        // 새로운 advisor 등록 시 캐시 비움
        Advisor a2 = mock(Advisor.class);
        when(a2.getPointcut()).thenReturn(pc);
        when(a2.getOrder()).thenReturn(2);

        reg.registerAdvisor(a2);
        assertTrue(cache.isEmpty(), "registerAdvisor 시 cachedAdvisors가 clear 되어야 함");
    }

    @Test
    @DisplayName("getAllAdvisors는 Unmodifiable 리스트를 반환한다")
    void allAdvisors_unmodifiable() {
        AdvisorRegistry reg = new AdvisorRegistry();
        Advisor a = mock(Advisor.class);
        reg.registerAdvisor(a);

        List<Advisor> list = reg.getAllAdvisors();
        assertThrows(UnsupportedOperationException.class, () -> list.add(a));
    }
}
