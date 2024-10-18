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
import view.InputHandler;

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
        Container.getInstance().scan(PackageName.view.getPackageName());

        Collection<Object> components = Container.getInstance().getComponents();
        for (Object component : components) {
            System.out.println(component.getClass().getName());
        }

        RequestHandler handler = Container.getInstance().get(RequestHandler.class);
        handler.setControllers(Container.getInstance().scanControllers());

        InputHandler inputHandler = Container.getInstance().get(InputHandler.class);
        inputHandler.startInputLoop();

    }

}
