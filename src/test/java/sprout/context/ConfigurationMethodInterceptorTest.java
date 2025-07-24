package sprout.context;

import net.sf.cglib.proxy.MethodProxy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import sprout.beans.annotation.Bean;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ConfigurationMethodInterceptorTest {

    // 테스트용 구성 클래스
    static class Conf {
        @Bean
        public String foo() { return "foo-created"; }

        @Bean("barName")
        public Integer bar() { return 42; }

        public String notBean() { return "nope"; }
    }

    private static Method m(Class<?> c, String name, Class<?>... types) {
        try { return c.getDeclaredMethod(name, types); }
        catch (Exception e) { throw new AssertionError(e); }
    }

    @Test
    @DisplayName("@Bean 아닌 경우: invokeSuper만 호출, 등록 안 함")
    void nonBean_callsSuperOnly() throws Throwable {
        BeanFactory bf = mock(BeanFactory.class);
        ConfigurationMethodInterceptor itc = new ConfigurationMethodInterceptor(bf);

        Conf conf = new Conf();
        Method method = m(Conf.class, "notBean");
        MethodProxy proxy = mock(MethodProxy.class);

        when(proxy.invokeSuper(conf, new Object[]{})).thenReturn("nope");

        Object ret = itc.intercept(conf, method, new Object[]{}, proxy);

        assertEquals("nope", ret);
        verify(proxy, times(1)).invokeSuper(conf, new Object[]{});
        verify(bf, never()).registerRuntimeBean(anyString(), any());
        verify(bf, never()).containsBean(anyString());
    }

    @Test
    @DisplayName("@Bean이고 이미 등록되어 있으면 기존 Bean 리턴, invokeSuper 호출 안 함")
    void beanAlreadyExists_returnsCached() throws Throwable {
        BeanFactory bf = mock(BeanFactory.class);
        ConfigurationMethodInterceptor itc = new ConfigurationMethodInterceptor(bf);

        Conf conf = new Conf();
        Method method = m(Conf.class, "foo"); // value="" → 이름은 "foo"
        MethodProxy proxy = mock(MethodProxy.class);

        when(bf.containsBean("foo")).thenReturn(true);
        when(bf.getBean("foo")).thenReturn("cached");

        Object ret = itc.intercept(conf, method, new Object[]{}, proxy);

        assertEquals("cached", ret);
        verify(proxy, never()).invokeSuper(any(), any());
        verify(bf, never()).registerRuntimeBean(anyString(), any());
    }

    @Test
    @DisplayName("@Bean이고 미등록이면 invokeSuper 후 registerRuntimeBean")
    void beanCreated_andRegistered() throws Throwable {
        BeanFactory bf = mock(BeanFactory.class);
        ConfigurationMethodInterceptor itc = new ConfigurationMethodInterceptor(bf);

        Conf conf = new Conf();
        Method method = m(Conf.class, "foo");
        MethodProxy proxy = mock(MethodProxy.class);

        when(bf.containsBean("foo")).thenReturn(false);
        when(proxy.invokeSuper(conf, new Object[]{})).thenReturn("foo-created");

        Object ret = itc.intercept(conf, method, new Object[]{}, proxy);

        assertEquals("foo-created", ret);
        verify(proxy, times(1)).invokeSuper(conf, new Object[]{});
        verify(bf).registerRuntimeBean("foo", "foo-created");
    }

    @Test
    @DisplayName("@Bean(value=\"name\") 사용 시 지정된 이름으로 등록/조회")
    void beanNameFromAnnotationValue() throws Throwable {
        BeanFactory bf = mock(BeanFactory.class);
        ConfigurationMethodInterceptor itc = new ConfigurationMethodInterceptor(bf);

        Conf conf = new Conf();
        Method method = m(Conf.class, "bar");
        MethodProxy proxy = mock(MethodProxy.class);

        when(bf.containsBean("barName")).thenReturn(false);
        when(proxy.invokeSuper(conf, new Object[]{})).thenReturn(42);

        Object ret = itc.intercept(conf, method, new Object[]{}, proxy);

        assertEquals(42, ret);
        verify(bf).registerRuntimeBean("barName", 42);
    }
}
