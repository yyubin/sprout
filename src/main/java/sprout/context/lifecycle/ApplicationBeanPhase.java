package sprout.context.lifecycle;

import sprout.beans.BeanDefinition;
import sprout.beans.internal.BeanGraph;
import sprout.context.BeanFactory;
import sprout.context.builtins.DefaultListableBeanFactory;

import java.util.List;

/**
 * 애플리케이션 빈 생성 단계
 * Infrastructure 빈과 BeanPostProcessor가 준비된 후 애플리케이션 빈들을 생성
 */
public class ApplicationBeanPhase implements BeanLifecyclePhase {

    @Override
    public String getName() {
        return "Application Bean Initialization";
    }

    @Override
    public int getOrder() {
        return 300;
    }

    @Override
    public void execute(PhaseContext context) {
        BeanFactory beanFactory = context.getBeanFactory();
        List<BeanDefinition> appDefs = context.getAppDefs();

        if (!(beanFactory instanceof DefaultListableBeanFactory)) {
            throw new IllegalStateException("BeanFactory must be DefaultListableBeanFactory");
        }

        DefaultListableBeanFactory factory = (DefaultListableBeanFactory) beanFactory;

        // 위상 정렬 후 순서대로 생성
        List<BeanDefinition> order = new BeanGraph(appDefs).topologicallySorted();
        order.forEach(factory::createBean);

        // List 주입 후처리
        factory.postProcessListInjections();
    }
}
