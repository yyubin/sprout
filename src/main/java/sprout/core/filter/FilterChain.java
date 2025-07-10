package sprout.core.filter;

import sprout.mvc.http.HttpRequest;
import sprout.mvc.http.HttpResponse;

import java.io.IOException;
import java.util.List;

public class FilterChain {
    private final List<Filter> filters;
    private final Dispatcher dispatcher;
    private int currentFilterIndex = 0;

    public FilterChain(List<Filter> filters, Dispatcher dispatcher) {
        this.filters = filters;
        this.dispatcher = dispatcher;
    }

    public void doFilter(HttpRequest request, HttpResponse response) throws IOException {
        if (currentFilterIndex < filters.size()) {
            filters.get(currentFilterIndex++).doFilter(request, response, this);
        } else {
            dispatcher.dispatch(request, response);
        }
    }
}
