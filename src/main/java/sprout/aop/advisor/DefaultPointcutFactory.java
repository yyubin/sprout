package sprout.aop.advisor;
import sprout.beans.annotation.Component;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;

@Component
public class DefaultPointcutFactory implements PointcutFactory {

    @Override
    public Pointcut createPointcut(Class<? extends Annotation>[] annotationTypes, String regexExpression) {
        List<Pointcut> combinedPointcuts = new ArrayList<>();

        // 1. 어노테이션 기반 Pointcut 처리
        if (annotationTypes != null && annotationTypes.length > 0) {
            for (Class<? extends Annotation> annoType : annotationTypes) {
                System.out.println(annoType.getName());
                combinedPointcuts.add(new AnnotationPointcut(annoType));
            }
        }



        // 2. 정규 표현식 기반 Pointcut 처리
        if (regexExpression != null && !regexExpression.trim().isEmpty()) {
            String trimmedRegex = regexExpression.trim();
            if (trimmedRegex.startsWith("regex:")) {
                String regex = trimmedRegex.substring("regex:".length());
                int lastDotIndex = regex.lastIndexOf('.');
                if (lastDotIndex == -1 || lastDotIndex == regex.length() - 1) { // .으로 끝나거나 .이 없는 경우
                    // 클래스 전체 또는 메서드 전체만 지정된 경우 등 유효성 검사 강화
                    // 예: "com.example.Service.*" (클래스만) 또는 ".*methodName" (메서드만)
                    // 여기서는 클래스와 메서드를 명확히 구분해야 하므로, 간단한 유효성 추가
                    throw new IllegalArgumentException("Invalid regex pointcut expression format. Must specify both class and method patterns (e.g., 'package.ClassName.methodName'): " + regexExpression);
                }
                String classRegex = regex.substring(0, lastDotIndex);
                String methodRegex = regex.substring(lastDotIndex + 1);
                combinedPointcuts.add(new RegexPointcut(classRegex, methodRegex));
            } else {
                // "regex:" 접두사가 없는 경우, 기본적으로 전체 정규식으로 처리 (더 유연하게)
                // 이 경우 클래스와 메서드 패턴을 사용자가 직접 분리하여 제공해야 함
                // 예: "com\.example\.service\.UserService\.+methodName"
                // 또는 더 간단하게: 메서드 이름만 매칭하는 패턴으로 간주
                // 여기서는 "regex:" 접두사가 없으면 기본적으로 클래스.메서드 패턴으로 간주
                int lastDotIndex = trimmedRegex.lastIndexOf('.');
                if (lastDotIndex != -1 && lastDotIndex < trimmedRegex.length() - 1) {
                    String classRegex = trimmedRegex.substring(0, lastDotIndex);
                    String methodRegex = trimmedRegex.substring(lastDotIndex + 1);
                    combinedPointcuts.add(new RegexPointcut(classRegex, methodRegex));
                } else {
                    // 유효한 클래스.메서드 패턴이 아니면 에러 또는 특정 기본값
                    throw new IllegalArgumentException("Invalid pointcut expression format. Must start with 'regex:' or follow 'package.ClassName.methodName' pattern: " + regexExpression);
                }
            }
        }

        if (combinedPointcuts.isEmpty()) {
            throw new IllegalArgumentException("At least one pointcut condition (annotation or regex) must be provided for an advice.");
        } else if (combinedPointcuts.size() == 1) {
            return combinedPointcuts.getFirst(); // 단일 조건이면 해당 Pointcut 반환
        } else {
            return new CompositePointcut(combinedPointcuts); // 여러 조건이면 CompositePointcut으로 묶어서 반환
        }
    }
}
