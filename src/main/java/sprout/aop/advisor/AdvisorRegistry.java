package sprout.aop.advisor;

import sprout.beans.InfrastructureBean;
import sprout.beans.annotation.Component;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class AdvisorRegistry implements InfrastructureBean {
    private final List<Advisor> advisors = new ArrayList<>();
    private final Map<Method, List<Advisor>> cachedAdvisors = new ConcurrentHashMap<>();

    public AdvisorRegistry() {
    }

    public void registerAdvisor(Advisor advisor) {
        synchronized (this) {
            advisors.add(advisor);
            cachedAdvisors.clear();
            advisors.sort(Comparator.comparingInt(Advisor::getOrder));
        }
    }

    public List<Advisor> getApplicableAdvisors(Class<?> targetClass, Method method) {
        List<Advisor> cached = cachedAdvisors.get(method);

        // FIX : 캐싱해놓고 안쓰고 있었음; wtf
        if (cached != null) {
            return cached;
        }

        List<Advisor> applicableAdvisors = new ArrayList<>();
        for (Advisor advisor : advisors) {
            if (advisor.getPointcut().matches(targetClass, method)) {
                applicableAdvisors.add(advisor);
            }
        }

        cachedAdvisors.put(method, applicableAdvisors);
        return applicableAdvisors;
    }

    public List<Advisor> getAllAdvisors() {
        return Collections.unmodifiableList(advisors);
    }
}
