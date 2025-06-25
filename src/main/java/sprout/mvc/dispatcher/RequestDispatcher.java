package sprout.mvc.dispatcher;

// Spring 의 DispatcherServlet

import com.fasterxml.jackson.core.JsonProcessingException;
import sprout.beans.annotation.Component;
import sprout.mvc.http.HttpRequest;
import sprout.mvc.http.HttpResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import sprout.mvc.http.ResponseEntity;
import sprout.mvc.http.ResponseResolver;
import sprout.mvc.http.parser.HttpRequestParser;
import sprout.mvc.mapping.HandlerMapping;
import sprout.mvc.invoke.HandlerMethodInvoker;
import sprout.mvc.invoke.HandlerMethod;
import app.exception.ExceptionProcessor;
import app.exception.BadRequestException;
import app.exception.UnsupportedHttpMethod;
import app.util.Session;

import java.util.List;

@Component
public class RequestDispatcher {

    private final HttpRequestParser parser;
    private final HandlerMapping mapping;
    private final HandlerMethodInvoker invoker;
    private final List<ResponseResolver> responseResolvers;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public RequestDispatcher(HttpRequestParser parser,
                             HandlerMapping mapping,
                             HandlerMethodInvoker invoker,
                             List<ResponseResolver> responseResolvers
    ) {
        this.parser = parser;
        this.mapping = mapping;
        this.invoker = invoker;
        this.responseResolvers = responseResolvers;
    }

    public ResponseEntity<?> dispatch(String raw) throws JsonProcessingException {
        try {
            HttpRequest<?> req = parser.parse(raw);
            // Session.setSessionId(req.getSessionId());
            HandlerMethod hm = mapping.findHandler(req.getPath(), req.getMethod());
            if (hm == null) throw new BadRequestException();
            Object returnValue = invoker.invoke(hm.requestMappingInfo(), req);
            for (ResponseResolver resolver : responseResolvers) {
                if (resolver.supports(returnValue)) {
                    // 찾으면 바로 변환하고 결과를 반환
                    return resolver.resolve(returnValue, req);
                }
            }
            throw new IllegalStateException("No suitable ResponseResolver found for return value: " + returnValue);
        } catch (UnsupportedHttpMethod | BadRequestException e) {
            return ResponseEntity.badRequest();
        } catch (Exception ex) {
            //String msg = exceptionProcessor.handleUndefinedException(ex);
            return ResponseEntity.badRequest();
        }
    }
}
