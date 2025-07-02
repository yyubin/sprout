package sprout.core.filter;

public interface FilterChain {
    void next(String rawRequest) throws Exception;
}
