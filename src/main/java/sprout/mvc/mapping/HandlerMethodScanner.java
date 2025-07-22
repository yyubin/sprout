package sprout.mvc.mapping;

import sprout.beans.annotation.Component;
import sprout.beans.annotation.Controller;
import sprout.context.BeanFactory;
import sprout.mvc.annotation.*;
import sprout.mvc.http.HttpMethod;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;

@Component
public class HandlerMethodScanner {
    private final RequestMappingRegistry requestMappingRegistry;
    private final PathPatternResolver pathPatternResolver;

    public HandlerMethodScanner(RequestMappingRegistry requestMappingRegistry, PathPatternResolver pathPatternResolver) {
        this.requestMappingRegistry = requestMappingRegistry;
        this.pathPatternResolver = pathPatternResolver;
    }

    public void scanControllers(BeanFactory context) {
        Collection<Object> beans = context.getAllBeans();
        System.out.println(beans.size() + " beans found");
        for (Object bean : beans) {
            Class<?> beanClass = bean.getClass();
            if (beanClass.isAnnotationPresent(Controller.class)) {
                System.out.println("found controller: " + beanClass.getName());
                String classLevelBasePath = extractBasePath(beanClass);
                for (Method method : beanClass.getMethods()) {
                    RequestMappingInfoExtractor requestMappingInfoExtractor = findRequestMappingInfoExtractor(method);
                    if (requestMappingInfoExtractor != null) {
                        String methodPath = requestMappingInfoExtractor.getPath();
                        HttpMethod[] httpMethods = requestMappingInfoExtractor.getHttpMethods();

                        String finalPathString = combinePaths(classLevelBasePath, methodPath);
                        PathPattern pathPattern = pathPatternResolver.resolve(finalPathString);

                        for (HttpMethod httpMethod : httpMethods) {
                            requestMappingRegistry.register(pathPattern, httpMethod, bean, method);
                        }
                    }
                }
            }
        }
    }

    String extractBasePath(Class<?> clazz) {
        RequestMapping requestMapping = clazz.getAnnotation(RequestMapping.class);
        if (requestMapping != null) {
            if (requestMapping.path().length > 0) {
                return requestMapping.path()[0];
            }
            if (requestMapping.value().length > 0) {
                return requestMapping.value()[0];
            }
        }
        return "";
    }

    public RequestMappingInfoExtractor findRequestMappingInfoExtractor(Method method) {
        for (Annotation ann : method.getDeclaredAnnotations()) {

            // 직접 선언된 @RequestMapping
            RequestMapping rm = ann instanceof RequestMapping
                    ? (RequestMapping) ann
                    : ann.annotationType().getAnnotation(RequestMapping.class);

            if (rm == null) continue;

            if (ann instanceof RequestMapping) {
                int cnt = rm.method().length;
                if (cnt != 1) { // 0 개(생략) -or- 2 개 이상이면 무시
                    System.out.printf(
                            "[WARN] %s.%s() - skipped: ambiguous @RequestMapping (method=%s)%n",
                            method.getDeclaringClass().getSimpleName(), method.getName(),
                            Arrays.toString(rm.method())
                    );
                    return null;
                }
            }

            // value() 가 비어 있으면 path() → value() 순으로 탐색
            String[] paths = extractPaths(ann, rm);

            HttpMethod[] methods = rm.method();
            if (methods.length == 0) methods = new HttpMethod[] { HttpMethod.GET };

            String path = (paths.length > 0 && !paths[0].isBlank()) ? paths[0] : "/";
            return new RequestMappingInfoExtractor(path, methods);
        }
        return null;
    }

    private String[] extractPaths(Annotation ann, RequestMapping fallback) {
        String[] p = getAttribute(ann, "value");
        if (p.length == 0 || p[0].isBlank()) p = getAttribute(ann, "path");
        if (p.length == 0 || p[0].isBlank()) p = fallback.path();
        if (p.length == 0 || p[0].isBlank()) p = fallback.value();
        return p;
    }

    private String[] getAttribute(Annotation ann, String attr) {
        try {
            Method m = ann.annotationType().getDeclaredMethod(attr);
            if (m.getReturnType().isArray()
                    && m.getReturnType().getComponentType() == String.class) {
                return (String[]) m.invoke(ann);
            }
        } catch (ReflectiveOperationException ignored) { }
        return new String[0];
    }


    public String combinePaths(String basePath, String methodPath) {
        if (basePath.isEmpty() || basePath.equals("/")) {
            return methodPath.startsWith("/") ? methodPath : "/" + methodPath;
        }
        if (methodPath.isEmpty() || methodPath.equals("/")) {
            return basePath.startsWith("/") ? basePath : "/" + basePath;
        }
        // 기본 경로가 /로 끝나지 않고 메서드 경로가 /로 시작하지 않는 경우 처리
        String normalizedBasePath = basePath.endsWith("/") ? basePath.substring(0, basePath.length() - 1) : basePath;
        String normalizedMethodPath = methodPath.startsWith("/") ? methodPath : "/" + methodPath;
        return normalizedBasePath + normalizedMethodPath;
    }

}
