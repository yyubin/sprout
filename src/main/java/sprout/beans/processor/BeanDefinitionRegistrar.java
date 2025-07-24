package sprout.beans.processor;

import sprout.beans.BeanDefinition;
import sprout.beans.InfrastructureBean;

import java.util.Collection;

public interface BeanDefinitionRegistrar extends InfrastructureBean {
    Collection<BeanDefinition> registerAdditionalBeanDefinitions(Collection<BeanDefinition> existingDefs) throws NoSuchMethodException;
}
