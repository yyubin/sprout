package sprout.mvc.dispatcher;

import sprout.beans.annotation.Component;
import sprout.core.filter.Dispatcher;
import sprout.core.filter.Filter;
import sprout.core.filter.FilterChain;
import sprout.core.interceptor.Interceptor;
import sprout.core.interceptor.InterceptorChain;
import sprout.mvc.advice.ControllerAdviceRegistry;
import sprout.mvc.advice.ExceptionHandlerObject;
import sprout.mvc.advice.ResponseAdvice;
import sprout.mvc.exception.ExceptionResolver;
import sprout.mvc.http.*;
import sprout.mvc.invoke.HandlerMethod;
import sprout.mvc.invoke.HandlerMethodInvoker;
import sprout.mvc.mapping.HandlerMapping;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;

@Component
public class RequestDispatcher {

    private final HandlerMapping mapping;
    private final HandlerMethodInvoker invoker;
    private final List<ResponseResolver> responseResolvers;
    private final List<ResponseAdvice> responseAdvices;
    private final List<Filter> filters;
    private final List<Interceptor> interceptors;
    private final List<ExceptionResolver> exceptionResolvers;
    private final List<DispatchHook> dispatchHooks;

    public RequestDispatcher(HandlerMapping mapping,
                             HandlerMethodInvoker invoker,
                             List<ResponseResolver> responseResolvers,
                             List<ResponseAdvice> responseAdvices,
                             List<Filter> filters,
                             List<Interceptor> interceptors,
                             List<ExceptionResolver> exceptionResolvers,
                             List<DispatchHook> dispatchHooks
    ) {
        this.mapping = mapping;
        this.invoker = invoker;
        this.responseResolvers = responseResolvers;
        this.responseAdvices = responseAdvices;
        this.filters = filters;
        this.interceptors = interceptors;
        this.exceptionResolvers = exceptionResolvers;
        this.dispatchHooks = dispatchHooks;
    }

    public void dispatch(HttpRequest<?> req, HttpResponse res) throws IOException {
        try {
            for (DispatchHook hook : dispatchHooks) {
                hook.beforeDispatch(req, res);
            }

            new FilterChain(filters, this::doDispatch).doFilter(req, res);
        } finally {
            for (DispatchHook hook : dispatchHooks) {
                hook.afterDispatch(req, res);
            }
        }

    }

    private void doDispatch(HttpRequest<?> req, HttpResponse res) {
        HandlerMethod hm = null;
        Exception caughtException = null;
        InterceptorChain interceptorChain = new InterceptorChain(interceptors);
        try {
            System.out.println(req.getPath() + "  " + req.getMethod().toString());
            hm = mapping.findHandler(req.getPath(), req.getMethod());
            if (hm == null) {
                // FIX: BadRequestException 대신 404 응답을 생성
                System.err.println("No handler found for: " + req.getMethod() + " " + req.getPath());
                res.setResponseEntity(
                        new ResponseEntity<>("Not Found", null, ResponseCode.NOT_FOUND)
                );
                return; // 핸들러가 없으므로 즉시 종료
            }

            if (!interceptorChain.applyPreHandle(req, res, hm)) {
                return;
            }

            Object returnValue = invoker.invoke(hm.requestMappingInfo(), req);
            interceptorChain.applyPostHandle(req, res, hm, returnValue);
            setResponseResolvers(returnValue, req, res);

        } catch (Exception e) { // 컨트롤러 또는 인터셉터에서 예외 발생 시
            caughtException = e;
            System.err.println("Exception caught in doDispatch: " + e.getMessage());
            e.printStackTrace(); // 디버깅용

            Object handledReturnValue = null;
            for (ExceptionResolver resolver : exceptionResolvers) {
                handledReturnValue = resolver.resolveException(req, res, hm, caughtException);
                if (handledReturnValue != null) {
                    // 예외 리졸버가 응답을 직접 설정했을 수도 있고, 반환 값을 주었을 수도 있음
                    // 반환 값이 있다면 ResponseResolvers로 처리
                    if (handledReturnValue instanceof ResponseEntity) { // 이미 ResponseEntity라면 직접 설정
                        res.setResponseEntity((ResponseEntity<?>) handledReturnValue);
                    } else {
                        setResponseResolvers(handledReturnValue, req, res);
                    }
                    return;
                }
            }
        } finally {
            if (hm != null) {
                interceptorChain.applyAfterCompletion(req, res, hm, caughtException);
            }
        }

    }

    private void setResponseResolvers(Object returnValue, HttpRequest<?> req, HttpResponse res) {
        if (res.isCommitted()) return; // 이미 응답 설정된 경우 무시

        Object processed = applyResponseAdvices(returnValue, req);

        for (ResponseResolver resolver : responseResolvers) {
            if (resolver.supports(processed)) {
                ResponseEntity<?> responseEntity = resolver.resolve(processed, req);
                res.setResponseEntity(responseEntity);
                return;
            }
        }

        throw new IllegalStateException("No suitable ResponseResolver found for return value: " + processed);
    }

    private Object applyResponseAdvices(Object returnValue, HttpRequest<?> req) {
        Object current = returnValue;
        for (ResponseAdvice advice : responseAdvices) {
            if (advice.supports(current, req)) {
                current = advice.beforeBodyWrite(current, req);
            }
        }
        return current;
    }

}
