package sprout.context.lifecycle;

import sprout.context.BeanFactory;
import sprout.context.ContextInitializer;

import java.util.List;

/**
 * ContextInitializer 실행 단계
 * 모든 빈이 생성된 후 최종 초기화 작업 수행
 */
public class ContextInitializerPhase implements BeanLifecyclePhase {

    @Override
    public String getName() {
        return "ContextInitializer Execution";
    }

    @Override
    public int getOrder() {
        return 400;
    }

    @Override
    public void execute(PhaseContext context) {
        BeanFactory beanFactory = context.getBeanFactory();

        // 모든 ContextInitializer를 찾아서 실행
        List<ContextInitializer> contextInitializers = beanFactory.getAllBeans(ContextInitializer.class);
        for (ContextInitializer initializer : contextInitializers) {
            initializer.initializeAfterRefresh(beanFactory);
        }
    }
}
