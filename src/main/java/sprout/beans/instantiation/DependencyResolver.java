package sprout.beans.instantiation;

import sprout.beans.BeanDefinition;

import java.lang.reflect.Parameter;

/**
 * 의존성 해결 인터페이스
 * 생성자나 팩토리 메서드의 파라미터들을 해결하는 역할
 */
public interface DependencyResolver {

    /**
     * 주어진 타입 배열과 파라미터 정보를 바탕으로 의존성을 해결합니다
     *
     * @param dependencyTypes 의존성 타입 배열
     * @param params 파라미터 정보
     * @param targetDef 현재 생성하려는 빈의 정의
     * @return 해결된 의존성 배열
     */
    Object[] resolve(Class<?>[] dependencyTypes, Parameter[] params, BeanDefinition targetDef);
}
