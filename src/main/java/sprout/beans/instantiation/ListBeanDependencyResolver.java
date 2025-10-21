package sprout.beans.instantiation;

import sprout.beans.BeanDefinition;
import sprout.context.PendingListInjection;

import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.List;

/**
 * List 타입 의존성을 해결하는 resolver
 * List 주입의 경우 나중에 postProcessListInjections에서 채워지므로
 * 빈 리스트를 반환하고 pending 목록에 등록
 */
public class ListBeanDependencyResolver implements DependencyTypeResolver {

    private final List<PendingListInjection> pendingListInjections;

    public ListBeanDependencyResolver(List<PendingListInjection> pendingListInjections) {
        this.pendingListInjections = pendingListInjections;
    }

    @Override
    public boolean supports(Class<?> type) {
        return List.class.isAssignableFrom(type);
    }

    @Override
    public Object resolve(Class<?> type, Parameter param, BeanDefinition targetDef) {
        List<Object> emptyList = new ArrayList<>();

        // 제네릭 타입 추출
        Class<?> genericType = (Class<?>) ((ParameterizedType) param.getParameterizedType())
                .getActualTypeArguments()[0];

        // 나중에 채울 수 있도록 pending 목록에 등록
        pendingListInjections.add(new PendingListInjection(null, emptyList, genericType));

        return emptyList;
    }
}
