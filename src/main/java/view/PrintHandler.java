package view;


import config.annotations.Component;
import config.annotations.Requires;
import http.response.HttpResponse;

@Component
@Requires(dependsOn = {})
public class PrintHandler {

    public <T> void printSuccessWithResponseCodeAndCustomMessage(HttpResponse<T> response) {
        System.out.println(response.getResponseCode().getCode() + " " + response.getDescription());
    }

    public <T> void printSuccessWithResponseCodeAndDefaultMessage(HttpResponse<T> httpResponse) {
        System.out.println(httpResponse.getResponseCode().getCode() + " " + httpResponse.getResponseCode().getMessage());
    }
}
