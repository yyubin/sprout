package sprout.context;

import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;
import sprout.beans.annotation.Bean;
import sprout.beans.annotation.Component;

import java.lang.reflect.Method;

@Component
public class ConfigurationMethodInterceptor implements MethodInterceptor {

    private final Container container;

    public ConfigurationMethodInterceptor(Container container) {
        this.container = container;
    }

    @Override
    public Object intercept(Object obj,
                            Method method,
                            Object[] args,
                            MethodProxy proxy) throws Throwable {

        // @Bean 메서드가 아니면 그대로 실행
        if (!method.isAnnotationPresent(Bean.class)) {
            return proxy.invokeSuper(obj, args);
        }

        // Bean 이름 결정 (@Bean(value="..") or 메서드 이름)
        String beanName = method.getAnnotation(Bean.class).value();
        if (beanName.isEmpty()) beanName = method.getName();

        // 이미 등록돼 있으면 반환
        if (container.containsBean(beanName)) {
            return container.get(beanName);
        }

        // 아직이면 실제 메서드 실행하여 빈 생성
        Object beanInstance = proxy.invokeSuper(obj, args);

        // 컨테이너에 등록 (싱글톤 보장)
        container.registerRuntimeBean(beanName, beanInstance);

        return beanInstance;
    }
}
