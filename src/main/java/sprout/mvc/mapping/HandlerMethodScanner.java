package sprout.mvc.mapping;

import sprout.beans.annotation.Component;
import sprout.beans.annotation.Controller;
import sprout.context.Container;
import sprout.mvc.annotation.*;
import sprout.mvc.http.HttpMethod;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Collection;

@Component
public class HandlerMethodScanner {
    private final RequestMappingRegistry requestMappingRegistry;
    private final Container container;

    public HandlerMethodScanner(RequestMappingRegistry requestMappingRegistry, Container container) {
        this.requestMappingRegistry = requestMappingRegistry;
        this.container = container;
    }

    public void scanControllers() {
        System.out.println("Scanning Controllers");
        Collection<Object> beans = container.beans();
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

                        String finalPath = combinePaths(classLevelBasePath, methodPath);

                        for (HttpMethod httpMethod : httpMethods) {
                            requestMappingRegistry.register(finalPath, httpMethod, bean, method);
                        }
                    }
                }
            }
        }
    }

    private String extractBasePath(Class<?> clazz) {
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

    private RequestMappingInfoExtractor findRequestMappingInfoExtractor(Method method) {
        for (Annotation methodAnnotation : method.getDeclaredAnnotations()) {
            if (methodAnnotation.annotationType().getPackage() != null &&
                    methodAnnotation.annotationType().getPackage().getName().startsWith("java.lang.annotation")) {
                continue;
            }
            RequestMapping requestMapping = methodAnnotation.annotationType().getAnnotation(RequestMapping.class);
            if (requestMapping != null) {
                String[] paths = getPathFromAnnotation(methodAnnotation);
                HttpMethod[] methods = requestMapping.method();

                if (paths.length == 0) {
                    paths = requestMapping.path();
                    if (paths.length == 0) {
                        paths = requestMapping.value();
                    }
                }

                String path = (paths.length > 0) ? paths[0] : "/";
                return new RequestMappingInfoExtractor(path, methods);
            }
        }
        return null;
    }

    private String[] getPathFromAnnotation(Annotation annotation) {
        try {
            Method valueMethod = annotation.annotationType().getDeclaredMethod("value");
            if (valueMethod.getReturnType().isArray() && valueMethod.getReturnType().getComponentType().equals(String.class)) {
                return (String[]) valueMethod.invoke(annotation);
            }
        } catch (NoSuchMethodException e) {
            try {
                Method pathMethod = annotation.annotationType().getDeclaredMethod("path");
                if (pathMethod.getReturnType().isArray() && pathMethod.getReturnType().getComponentType().equals(String.class)) {
                    return (String[]) pathMethod.invoke(annotation);
                }
            } catch (ReflectiveOperationException ignored) {
            }
        } catch (ReflectiveOperationException ignored) {
        }
        return new String[]{};
    }

    private String combinePaths(String basePath, String methodPath) {
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


    private static class RequestMappingInfoExtractor {
        private final String path;
        private final HttpMethod[] httpMethods;

        public RequestMappingInfoExtractor(String path, HttpMethod[] httpMethods) {
            this.path = path;
            this.httpMethods = httpMethods;
        }

        public String getPath() {
            return path;
        }

        public HttpMethod[] getHttpMethods() {
            return httpMethods;
        }
    }
}
