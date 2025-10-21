package sprout.beans.instantiation;

import sprout.beans.BeanDefinition;

import java.lang.reflect.Parameter;
import java.util.List;

/**
 * 여러 DependencyTypeResolver를 조합하여 사용하는 Composite Resolver
 * Chain of Responsibility 패턴 적용
 */
public class CompositeDependencyResolver implements DependencyResolver {

    private final List<DependencyTypeResolver> typeResolvers;

    public CompositeDependencyResolver(List<DependencyTypeResolver> typeResolvers) {
        this.typeResolvers = typeResolvers;
    }

    @Override
    public Object[] resolve(Class<?>[] dependencyTypes, Parameter[] params, BeanDefinition targetDef) {
        Object[] deps = new Object[dependencyTypes.length];

        for (int i = 0; i < dependencyTypes.length; i++) {
            Class<?> paramType = dependencyTypes[i];
            Parameter param = params[i];

            // 적절한 resolver를 찾아서 의존성 해결
            Object resolved = null;
            for (DependencyTypeResolver resolver : typeResolvers) {
                if (resolver.supports(paramType)) {
                    resolved = resolver.resolve(paramType, param, targetDef);
                    break;
                }
            }

            if (resolved == null) {
                throw new RuntimeException(
                        "No DependencyTypeResolver found for type: " + paramType.getName() +
                                " in bean: " + targetDef.getName()
                );
            }

            deps[i] = resolved;
        }

        return deps;
    }
}
