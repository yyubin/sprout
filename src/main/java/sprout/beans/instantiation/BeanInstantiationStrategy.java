package sprout.beans.instantiation;

import sprout.beans.BeanCreationMethod;
import sprout.beans.BeanDefinition;
import sprout.context.BeanFactory;

/**
 * 빈 인스턴스화 전략 인터페이스
 * 다양한 방식으로 빈을 생성할 수 있도록 Strategy Pattern을 적용
 */
public interface BeanInstantiationStrategy {

    /**
     * 빈 인스턴스를 생성합니다
     *
     * @param def 빈 정의
     * @param dependencyResolver 의존성 해결자
     * @param beanFactory 빈 팩토리
     * @return 생성된 빈 인스턴스
     */
    Object instantiate(BeanDefinition def, DependencyResolver dependencyResolver, BeanFactory beanFactory) throws Exception;

    /**
     * 이 전략이 주어진 빈 생성 방식을 지원하는지 확인
     *
     * @param method 빈 생성 방식
     * @return 지원 여부
     */
    boolean supports(BeanCreationMethod method);
}
