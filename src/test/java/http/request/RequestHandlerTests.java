package http.request;

import config.Container;
import config.PackageName;
import sprout.mvc.mapping.ControllerInterface;
import exception.BadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

public class RequestHandlerTests {

    private RequestHandler requestHandler;

    @BeforeEach
    void setUp() throws Exception {
        Container.getInstance().scan(PackageName.repository.getPackageName());
        Container.getInstance().scan(PackageName.config_exception.getPackageName());
        Container.getInstance().scan(PackageName.http_request.getPackageName());
        Container.getInstance().scan(PackageName.util.getPackageName());
        Container.getInstance().scan(PackageName.service.getPackageName());
        Container.getInstance().scan(PackageName.view.getPackageName());
        Container.getInstance().scan(PackageName.controller.getPackageName());
        requestHandler = Container.getInstance().get(RequestHandler.class);
    }

    @Test
    void testHandleGetRequest() throws Exception {
        List<ControllerInterface> controllers = new ArrayList<>();
        GetController getController = mock(GetController.class);
        controllers.add(getController);
        requestHandler.setControllers(controllers);


        String rawRequest = "GET /testPath HTTP/1.1\n";

        requestHandler.handleRequest(rawRequest);

        verify(getController, times(1)).getMethodWithoutParams();
    }

    @Test
    void testHandlePostRequest() throws Exception {
        List<ControllerInterface> controllers = new ArrayList<>();
        PostController postController = mock(PostController.class);
        controllers.add(postController);
        requestHandler.setControllers(controllers);

        String rawRequest = "POST /postPath HTTP/1.1\n" +
                            "{\"key\":\"value\"}";

        requestHandler.handleRequest(rawRequest);

        verify(postController, times(1)).postMethodWithString(any(AnyModel.class));
    }

    @Test
    void testHandlePostRequestByQuery() throws Exception {
        List<ControllerInterface> controllers = new ArrayList<>();
        PostController postController = mock(PostController.class);
        controllers.add(postController);
        requestHandler.setControllers(controllers);

        String rawRequest = "POST /postPathWithoutModel HTTP/1.1\n"+
                                "{\"key\":\"value\"}";

        requestHandler.handleRequest(rawRequest);

        verify(postController, times(1)).postMethodWithoutModel(any());
    }

    @Test
    void testHandleNoMatchingPath() {
        List<ControllerInterface> controllers = new ArrayList<>();
        controllers.add(new GetController());
        requestHandler.setControllers(controllers);

        String rawRequest = "GET /nonMatchingPath\n";

        Exception exception = assertThrows(BadRequestException.class, () -> {
            requestHandler.handleRequest(rawRequest);
        });

    }

}
