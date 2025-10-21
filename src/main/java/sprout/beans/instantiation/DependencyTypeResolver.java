package sprout.beans.instantiation;

import sprout.beans.BeanDefinition;

import java.lang.reflect.Parameter;

// 특정 타입의 의존성을 해결하는 전략 인터페이스
public interface DependencyTypeResolver {

    /**
     * 주어진 타입을 이 resolver가 처리할 수 있는지 확인
     *
     * @param type 의존성 타입
     * @return 지원 여부
     */
    boolean supports(Class<?> type);

    /**
     * 의존성을 해결하여 반환
     *
     * @param type 의존성 타입
     * @param param 파라미터 정보 (제네릭 타입 등 추출용)
     * @param targetDef 현재 생성하려는 빈의 정의
     * @return 해결된 의존성 객체
     */
    Object resolve(Class<?> type, Parameter param, BeanDefinition targetDef);
}
