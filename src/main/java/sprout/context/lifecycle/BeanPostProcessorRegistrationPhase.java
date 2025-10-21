package sprout.context.lifecycle;

import sprout.beans.processor.BeanPostProcessor;
import sprout.context.BeanFactory;
import sprout.context.builtins.DefaultListableBeanFactory;

import java.util.List;

/**
 * BeanPostProcessor 등록 단계
 * 애플리케이션 빈 생성 전에 모든 BeanPostProcessor를 등록
 */
public class BeanPostProcessorRegistrationPhase implements BeanLifecyclePhase {

    @Override
    public String getName() {
        return "BeanPostProcessor Registration";
    }

    @Override
    public int getOrder() {
        return 200;
    }

    @Override
    public void execute(PhaseContext context) {
        BeanFactory beanFactory = context.getBeanFactory();

        if (!(beanFactory instanceof DefaultListableBeanFactory)) {
            throw new IllegalStateException("BeanFactory must be DefaultListableBeanFactory");
        }

        DefaultListableBeanFactory factory = (DefaultListableBeanFactory) beanFactory;

        // 모든 BeanPostProcessor를 찾아서 등록
        List<BeanPostProcessor> allBeanPostProcessor = beanFactory.getAllBeans(BeanPostProcessor.class);

        for (BeanPostProcessor beanPostProcessor : allBeanPostProcessor) {
            factory.addBeanPostProcessor(beanPostProcessor);
        }
    }
}
