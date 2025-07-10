//package legacy.view;
//
//
//import sprout.beans.annotation.Component;
//import legacy.config.annotations.Priority;
//import sprout.beans.annotation.Requires;
//import sprout.mvc.http.HttpResponse;
//import app.message.ExceptionMessage;
//import legacy.view.interfaces.PrintProcessor;
//
//@Component
//@Priority(value = 0)
//@Requires(dependsOn = {})
//public class PrintHandler implements PrintProcessor {
//
//    public void printCustomMessage(String message) {
//        System.out.println(message);
//    }
//
//    public void printExceptionMessage(Throwable e) {
//        if(e.getMessage().isEmpty()) {
//            System.out.println(e.getMessage());
//            return;
//        }
//        System.out.println(ExceptionMessage.BAD_REQUEST);
//    }
//
//    public <T> void printSuccessWithResponseCodeAndCustomMessage(HttpResponse<T> response) {
//        System.out.println(response.getResponseCode().getCode() + " " + response.getDescription());
//    }
//
//    public <T> void printSuccessWithResponseCodeAndDefaultMessage(HttpResponse<T> httpResponse) {
//        System.out.println(httpResponse.getResponseCode().getCode() + " " + httpResponse.getResponseCode().getMessage());
//    }
//
////    public void printResponseBodyAsMap(HttpResponse<Map<String, Object>> response) {
////        Map<String, Object> body = response.getBody();
////        if (body != null) {
////            body.forEach((key, value) -> {
////                System.out.println(key + " : " + value);
////            });
////        }
////    }
////
////    public void printResponseBodyAsMapList(HttpResponse<List<Map<String, Object>>> response) {
////        List<Map<String, Object>> body = response.getBody();
////        for (Map<String, Object> map : body) {
////            map.forEach((key, value) -> {
////                System.out.print(key + " : " + value + " ");
////            });
////            System.out.println();
////        }
////    }
//
//}
