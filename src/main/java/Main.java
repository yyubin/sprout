import com.fasterxml.jackson.core.JsonProcessingException;
import exception.BadRequestException;
import http.request.HttpRequest;
import http.request.HttpRequestParser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class Main {

    public static void main(String[] args) throws IOException {

        try {
            System.out.println("HTTP Request 형식으로 입력해주세요: ");

            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            StringBuilder rawRequest = new StringBuilder();
            String line;

            while (!(line = reader.readLine()).isEmpty()) {
                rawRequest.append(line).append("\n");
            }

            // 파싱
            HttpRequest<?> httpRequest = HttpRequestParser.parse(rawRequest.toString(), Object.class);

            // 결과 출력
            System.out.println("Parsed HttpRequest:");
            System.out.println("Method: " + httpRequest.getMethod());
            System.out.println("Path: " + httpRequest.getPath());
            System.out.println("Body: " + (httpRequest.getBody() != null ? httpRequest.getBody().toString() : "No Body"));
            System.out.println("Query Params: " + httpRequest.getQueryParams());
        } catch (IOException e) {
            System.out.println("입력 오류: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("파싱 오류: " + e.getMessage());
        }
    }

}
