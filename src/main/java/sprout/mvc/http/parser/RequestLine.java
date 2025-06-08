package sprout.mvc.http.parser;

import sprout.mvc.http.HttpMethod;

public record RequestLine(HttpMethod method, String rawPath, String cleanPath) {}