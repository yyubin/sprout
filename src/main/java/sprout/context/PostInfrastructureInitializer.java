package sprout.context;

import sprout.beans.InfrastructureBean;

import java.util.List;

// FIX : InfraStructure 생성 이후 사용하기 때문에, 반드시 *InfrastructureBean*
public interface PostInfrastructureInitializer extends InfrastructureBean {
    void afterInfrastructureSetup(BeanFactory beanFactory, List<String> basePackages);
}
