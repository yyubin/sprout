package sprout.beans.processor;

import sprout.beans.BeanDefinition;

import java.util.Collection;

public interface BeanDefinitionRegistrar {
    Collection<BeanDefinition> registerAdditionalBeanDefinitions(Collection<BeanDefinition> existingDefs) throws NoSuchMethodException;
}
