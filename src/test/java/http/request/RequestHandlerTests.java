package http.request;

import controller.ControllerInterface;
import controller.ExampleController;
import controller.annotations.GetMapping;
import controller.annotations.PostMapping;
import exception.NoMatchingHandlerException;
import message.ExceptionMessage;
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
    void setUp() {
        requestHandler = new RequestHandler();
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

        String rawRequest = "POST /postPath\n";

        requestHandler.handleRequest(rawRequest);

        verify(postController, times(1)).postMethodWithHttpRequest(any(HttpRequest.class));
    }

    @Test
    void testHandleNoMatchingPath() {
        List<ControllerInterface> controllers = new ArrayList<>();
        controllers.add(new GetController());
        requestHandler.setControllers(controllers);

        String rawRequest = "GET /nonMatchingPath\n";

        Exception exception = assertThrows(NoMatchingHandlerException.class, () -> {
            requestHandler.handleRequest(rawRequest);
        });

        assertEquals(ExceptionMessage.NO_MATCHING_PATH, exception.getMessage());
    }

}
