package sprout.core.filter;

import sprout.mvc.dispatcher.RequestDispatcher;

import java.util.Iterator;

public class DefaultFilterChain implements FilterChain{
    private final Iterator<Filter> it;
    private final RequestDispatcher dispatcher;

    public DefaultFilterChain(Iterator<Filter> it, RequestDispatcher dispatcher) {
        this.it = it;
        this.dispatcher = dispatcher;
    }

    @Override
    public void next(String rawRequest) throws Exception {
        if (it.hasNext()) {
            it.next().doFilter(rawRequest, this);
        } else {
            dispatcher.dispatch(rawRequest);  // 체인 끝 → 비즈 로직
        }
    }
}
