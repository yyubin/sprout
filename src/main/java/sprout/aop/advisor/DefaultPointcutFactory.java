package sprout.aop.advisor;
import sprout.beans.InfrastructureBean;
import sprout.beans.annotation.Component;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;

@Component
public class DefaultPointcutFactory implements PointcutFactory, InfrastructureBean {

    @Override
    public Pointcut createPointcut(Class<? extends Annotation>[] annotationTypes, String aspectjExpr) {
        List<Pointcut> pcs = new ArrayList<>();

        // 1) 어노테이션 조건들 (@Before.annotation() 등에 전달된 것)
        if (annotationTypes != null && annotationTypes.length > 0) {
            for (Class<? extends Annotation> anno : annotationTypes) {
                pcs.add(new AnnotationPointcut(anno));
            }
        }

        // 2) AspectJ 표현식
        if (aspectjExpr != null && !aspectjExpr.isBlank()) {
            pcs.add(new AspectJPointcutAdapter(aspectjExpr.trim()));
        }

        if (pcs.isEmpty()) {
            throw new IllegalArgumentException("At least one of annotation[] or pointcut() must be provided.");
        }
        return pcs.size() == 1 ? pcs.get(0) : new CompositePointcut(pcs); // AND 조건
    }
}