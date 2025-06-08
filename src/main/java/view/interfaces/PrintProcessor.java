package view.interfaces;

import sprout.mvc.http.HttpResponse;

public interface PrintProcessor {
    void printCustomMessage(String message);
    void printExceptionMessage(Throwable e);
    <T> void printSuccessWithResponseCodeAndCustomMessage(HttpResponse<T> response);
    <T> void printSuccessWithResponseCodeAndDefaultMessage(HttpResponse<T> response);
//    void printResponseBodyAsMap(HttpResponse<Map<String, Object>> response);
//    void printResponseBodyAsMapList(HttpResponse<List<Map<String, Object>>> response);
}
