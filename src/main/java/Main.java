import com.fasterxml.jackson.core.JsonProcessingException;
import config.Container;
import config.PackageName;
import controller.ControllerInterface;
import controller.ExampleController;
import controller.MemberController;
import exception.BadRequestException;
import http.request.HttpRequest;
import http.request.HttpRequestParser;
import http.request.RequestHandler;
import repository.InMemoryMemberRepository;
import service.MemberService;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collection;

public class Main {

    public static void main(String[] args) throws Exception {

        Container.getInstance().scan(PackageName.repository.getPackageName());
        Container.getInstance().scan(PackageName.http_request.getPackageName());
        Container.getInstance().scan(PackageName.util.getPackageName());
        Container.getInstance().scan(PackageName.service.getPackageName());
        Container.getInstance().scan(PackageName.controller.getPackageName());

        Collection<Object> components = Container.getInstance().getComponents();
        for (Object component : components) {
            System.out.println(component.getClass().getName());
        }

        System.out.println("HTTP Request 형식으로 입력해주세요: ");

        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        StringBuilder rawRequest = new StringBuilder();
        String line;

        while (!(line = reader.readLine()).isEmpty()) {
            rawRequest.append(line).append("\n");
        }

        RequestHandler handler = Container.getInstance().get(RequestHandler.class);
        handler.setControllers(Container.getInstance().scanControllers());
        handler.handleRequest(rawRequest.toString());
    }

}
