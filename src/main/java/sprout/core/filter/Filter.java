package sprout.core.filter;

public interface Filter {
    void doFilter(String rawRequest, FilterChain filterChain) throws Exception;
}
