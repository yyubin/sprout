package sprout.aop.advisor;

import sprout.beans.InfrastructureBean;
import sprout.beans.annotation.Component;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class AdvisorRegistry implements InfrastructureBean {
    private final List<Advisor> advisors = new ArrayList<>();
    private final Map<Class<?>, List<Advisor>> cachedAdvisors = new ConcurrentHashMap<>();

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
        List<Advisor> cachedForClass = cachedAdvisors.get(targetClass);
        if (cachedForClass != null) {};

        List<Advisor> applicableAdvisors = new ArrayList<>();
        for (Advisor advisor : advisors) {
            if (advisor.getPointcut().matches(targetClass, method)) {
                applicableAdvisors.add(advisor);
            }
        }

        applicableAdvisors.sort(Comparator.comparingInt(Advisor::getOrder));
        cachedAdvisors.put(targetClass, applicableAdvisors);
        return applicableAdvisors;
    }

    public List<Advisor> getAllAdvisors() {
        return Collections.unmodifiableList(advisors);
    }
}
