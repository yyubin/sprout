package sprout.mvc.dispatcher;

// Spring 의 DispatcherServlet

import sprout.mvc.http.HttpRequest;
import http.request.HttpRequestParser;
import sprout.mvc.http.HttpResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import sprout.mvc.mapping.HandlerMapping;
import sprout.mvc.invoke.HandlerMethodInvoker;
import sprout.mvc.invoke.HandlerMethod;
import config.exception.ExceptionProcessor;
import exception.BadRequestException;
import exception.UnsupportedHttpMethod;
import util.Session;

public class RequestDispatcher {
    private final HttpRequestParser parser;
    private final HandlerMapping mapping;
    private final HandlerMethodInvoker invoker;
    private final ExceptionProcessor exceptionProcessor;
    private final ObjectMapper objectMapper;

    public RequestDispatcher(HttpRequestParser parser,
                             HandlerMapping mapping,
                             HandlerMethodInvoker invoker,
                             ExceptionProcessor exceptionProcessor,
                             ObjectMapper objectMapper) {
        this.parser = parser;
        this.mapping = mapping;
        this.invoker = invoker;
        this.exceptionProcessor = exceptionProcessor;
        this.objectMapper = objectMapper;
    }

    /** Main entry: raw HTTP string → HttpResponse */
    public HttpResponse dispatch(String raw) {
        try {
            HttpRequest<?> req = parser.parse(raw);
            Session.setSessionId(req.getSessionId());
            HandlerMethod hm = mapping.findHandler(req.getPath(), req.getMethod());
            if (hm == null) throw new BadRequestException();
            Object result = invoker.invoke(hm, (HttpRequest) req);
            // serialize JSON response for now
            String body = objectMapper.writeValueAsString(result);
            return HttpResponse.ok(body);
        } catch (UnsupportedHttpMethod | BadRequestException e) {
            return HttpResponse.badRequest(e.getMessage());
        } catch (Exception ex) {
            String msg = exceptionProcessor.handleUndefinedException(ex);
            return HttpResponse.serverError(msg);
        }
    }
}
