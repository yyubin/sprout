package sprout.mvc.mapping;

import sprout.mvc.http.HttpMethod;

public class RequestMappingInfoExtractor {
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
