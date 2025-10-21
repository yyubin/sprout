package sprout.context.lifecycle;

import sprout.beans.BeanDefinition;
import sprout.beans.internal.BeanGraph;
import sprout.context.BeanFactory;
import sprout.context.PostInfrastructureInitializer;
import sprout.context.builtins.DefaultListableBeanFactory;

import java.util.List;

/**
 * 인프라 빈 생성 및 초기화 단계
 * BeanPostProcessor, InfrastructureBean 등을 먼저 생성
 */
public class InfrastructureBeanPhase implements BeanLifecyclePhase {

    @Override
    public String getName() {
        return "Infrastructure Bean Initialization";
    }

    @Override
    public int getOrder() {
        return 100;
    }

    @Override
    public void execute(PhaseContext context) throws Exception {
        BeanFactory beanFactory = context.getBeanFactory();
        List<BeanDefinition> infraDefs = context.getInfraDefs();

        if (!(beanFactory instanceof DefaultListableBeanFactory)) {
            throw new IllegalStateException("BeanFactory must be DefaultListableBeanFactory");
        }

        DefaultListableBeanFactory factory = (DefaultListableBeanFactory) beanFactory;

        // 위상 정렬 후 순서대로 생성
        List<BeanDefinition> order = new BeanGraph(infraDefs).topologicallySorted();
        order.forEach(factory::createBean);

        // List 주입 후처리
        factory.postProcessListInjections();

        // PostInfrastructureInitializer 실행
        List<PostInfrastructureInitializer> initializers = beanFactory.getAllBeans(PostInfrastructureInitializer.class);
        System.out.println("initializers: " + initializers);
        for (PostInfrastructureInitializer initializer : initializers) {
            initializer.afterInfrastructureSetup(beanFactory, context.getBasePackages());
        }
    }
}
