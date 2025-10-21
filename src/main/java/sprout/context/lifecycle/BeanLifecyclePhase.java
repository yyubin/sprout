package sprout.context.lifecycle;

import sprout.beans.BeanDefinition;
import sprout.context.BeanFactory;

import java.util.List;


public interface BeanLifecyclePhase {

    // 단계 이름
    String getName();

    // 실행 순서
    int getOrder();

    // 단계 실행
    void execute(PhaseContext context) throws Exception;

    // 단계 실행에 필요한 컨텍스트 정보
    class PhaseContext {
        private final BeanFactory beanFactory;
        private final List<BeanDefinition> infraDefs;
        private final List<BeanDefinition> appDefs;
        private final List<String> basePackages;

        public PhaseContext(BeanFactory beanFactory, List<BeanDefinition> infraDefs,
                            List<BeanDefinition> appDefs, List<String> basePackages) {
            this.beanFactory = beanFactory;
            this.infraDefs = infraDefs;
            this.appDefs = appDefs;
            this.basePackages = basePackages;
        }

        public BeanFactory getBeanFactory() {
            return beanFactory;
        }

        public List<BeanDefinition> getInfraDefs() {
            return infraDefs;
        }

        public List<BeanDefinition> getAppDefs() {
            return appDefs;
        }

        public List<String> getBasePackages() {
            return basePackages;
        }
    }
}
