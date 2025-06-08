package sprout.mvc.http.parser;

import legacy.http.request.ObjectMapperConfig;
import sprout.beans.annotation.Component;
import sprout.beans.annotation.Requires;
import sprout.mvc.http.HttpRequest;

import java.util.Map;

@Component
@Requires(dependsOn = {ObjectMapperConfig.class})
public class HttpRequestParser {
    private final RequestLineParser lineParser = new RequestLineParser();
    private final QueryStringParser qsParser = new QueryStringParser();
    private final BodyConverter bodyConverter;

    public HttpRequestParser(ObjectMapperConfig omConfig) {
        this.bodyConverter = new BodyConverter(omConfig.getObjectMapper());
    }

    public HttpRequest<Map<String,Object>> parse(String raw) {
        String[] parts = split(raw);
        String headerPart = parts[0];
        String bodyPart   = parts[1];
        String firstLine  = headerPart.split("\r?\n",2)[0];
        var rl    = lineParser.parse(firstLine);
        var query = qsParser.parse(rl.rawPath());
        var body  = bodyConverter.toMap(bodyPart, rl.method());
        return new HttpRequest<>(rl.method(), rl.cleanPath(), body, query);
    }

    private String[] split(String raw) {
        // 헤더·바디 구분 문자열을 먼저 CRLF( \r\n\r\n )로 찾고
        // 없으면 LF( \n\n ) 로 한 번 더 찾기
        int delimiterIdx   = raw.indexOf("\r\n\r\n");
        int delimiterLen   = 4;                 // 기본 CRLF 길이

        if (delimiterIdx == -1) {               // CRLF 구분자가 없다면
            delimiterIdx = raw.indexOf("\n\n"); // LF 구분자 탐색
            delimiterLen = 2;
        }

        if (delimiterIdx != -1) {
            return new String[]{
                    raw.substring(0, delimiterIdx),               // 헤더(요청라인+헤더)
                    raw.substring(delimiterIdx + delimiterLen)     // 바디
            };
        }
        // 구분자를 찾지 못했으면 바디는 빈 문자열
        return new String[]{ raw, "" };
    }
}