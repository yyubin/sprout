package view;


import config.annotations.Component;
import config.annotations.Requires;
import http.response.HttpResponse;

import java.util.List;
import java.util.Map;

@Component
@Requires(dependsOn = {})
public class PrintHandler {

    public void printCustomMessage(String message) {
        System.out.println(message);
    }

    public <T> void printSuccessWithResponseCodeAndCustomMessage(HttpResponse<T> response) {
        System.out.println(response.getResponseCode().getCode() + " " + response.getDescription());
    }

    public <T> void printSuccessWithResponseCodeAndDefaultMessage(HttpResponse<T> httpResponse) {
        System.out.println(httpResponse.getResponseCode().getCode() + " " + httpResponse.getResponseCode().getMessage());
    }

    public void printResponseBodyAsMap(HttpResponse<Map<String, Object>> response) {
        Map<String, Object> body = response.getBody();
        if (body != null) {
            body.forEach((key, value) -> {
                System.out.println(key + " : " + value);
            });
        }
    }

    public void printResponseBodyAsMapList(HttpResponse<List<Map<String, Object>>> response) {
        List<Map<String, Object>> body = response.getBody();
        for (Map<String, Object> map : body) {
            map.forEach((key, value) -> {
                System.out.print(key + " : " + value + " ");
            });
            System.out.println();
        }
    }

}
