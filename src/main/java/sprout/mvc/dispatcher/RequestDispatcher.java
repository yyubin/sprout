package sprout.mvc.dispatcher;

import sprout.beans.annotation.Component;
import sprout.core.filter.Filter;
import sprout.core.filter.FilterChain;
import sprout.core.interceptor.Interceptor;
import sprout.core.interceptor.InterceptorChain;
import sprout.mvc.http.HttpRequest;
import sprout.mvc.http.HttpResponse;
import sprout.mvc.http.ResponseEntity;
import sprout.mvc.http.ResponseResolver;
import sprout.mvc.invoke.HandlerMethod;
import sprout.mvc.invoke.HandlerMethodInvoker;
import sprout.mvc.mapping.HandlerMapping;
import app.exception.BadRequestException;

import java.io.IOException;
import java.util.List;

@Component
public class RequestDispatcher {

    private final HandlerMapping mapping;
    private final HandlerMethodInvoker invoker;
    private final List<ResponseResolver> responseResolvers;
    private final List<Filter> filters;
    private final List<Interceptor> interceptors;

    public RequestDispatcher(HandlerMapping mapping,
                             HandlerMethodInvoker invoker,
                             List<ResponseResolver> responseResolvers,
                             List<Filter> filters,
                             List<Interceptor> interceptors
    ) {
        this.mapping = mapping;
        this.invoker = invoker;
        this.responseResolvers = responseResolvers;
        this.filters = filters;
        this.interceptors = interceptors;
    }

    public void dispatch(HttpRequest<?> req, HttpResponse res) throws IOException {
        new FilterChain(filters, this::doDispatch).doFilter(req, res);
    }

    private void doDispatch(HttpRequest<?> req, HttpResponse res) {
        HandlerMethod hm = null;
        Exception dispatchException = null;
        try {
            hm = mapping.findHandler(req.getPath(), req.getMethod());
            if (hm == null) throw new BadRequestException();

            InterceptorChain interceptorChain = new InterceptorChain(interceptors);
            if (!interceptorChain.applyPreHandle(req, res, hm)) {
                return;
            }

            Object returnValue = invoker.invoke(hm.requestMappingInfo(), req);
            interceptorChain.applyPostHandle(req, res, hm, returnValue);

            for (ResponseResolver resolver : responseResolvers) {
                if (resolver.supports(returnValue)) {
                    ResponseEntity<?> responseEntity = resolver.resolve(returnValue, req);
                    res.setResponseEntity(responseEntity);
                    return;
                }
            }
            throw new IllegalStateException("No suitable ResponseResolver found for return value: " + returnValue);
        } catch (Exception e) {
            dispatchException = e;
        } finally {
            if (hm != null) {
                InterceptorChain interceptorChain = new InterceptorChain(interceptors);
                interceptorChain.applyAfterCompletion(req, res, hm, dispatchException);
            }
        }
    }
}
