package sprout.mvc.http.parser;


import sprout.beans.annotation.Component;
import sprout.mvc.exception.BadRequestException;
import sprout.mvc.exception.ExceptionMessage;
import sprout.mvc.http.HttpMethod;
import sprout.mvc.http.ResponseCode;

@Component
public class RequestLineParser {
    public RequestLine parse(String line) {
        String[] parts = line.trim().split(" ", 3);
        if (parts.length < 2) {
            throw new BadRequestException(ExceptionMessage.BAD_REQUEST, ResponseCode.BAD_REQUEST);
        }
        HttpMethod method = HttpMethod.valueOf(parts[0].toUpperCase());
        String rawPath   = parts[1];
        String cleanPath = rawPath.split("\\?")[0];
        return new RequestLine(method, rawPath, cleanPath);
    }
}