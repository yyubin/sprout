package view;

import legacy.view.PrintHandler;
import sprout.mvc.http.HttpResponse;
import sprout.mvc.http.ResponseCode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PrintHandlerTests {


    private PrintHandler printHandler;
    private final ByteArrayOutputStream outputStreamCaptor = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;

    @BeforeEach
    void setUp() {
        printHandler = new PrintHandler();
        System.setOut(new PrintStream(outputStreamCaptor));
    }

    @Test
    void testPrintCustomMessage() {
        String message = "Hello, World!";
        printHandler.printCustomMessage(message);
        assertEquals("Hello, World!\n", outputStreamCaptor.toString());
    }

    @Test
    void testPrintSuccessWithResponseCodeAndCustomMessage() {
        HttpResponse<String> response = new HttpResponse<>("Custom Success Message", ResponseCode.SUCCESS, null);
        printHandler.printSuccessWithResponseCodeAndCustomMessage(response);
        assertEquals("200 Custom Success Message\n", outputStreamCaptor.toString());
    }

    @Test
    void testPrintSuccessWithResponseCodeAndDefaultMessage() {
        HttpResponse<String> response = new HttpResponse<>(ResponseCode.SUCCESS.getMessage(), ResponseCode.SUCCESS, null);
        printHandler.printSuccessWithResponseCodeAndDefaultMessage(response);
        assertEquals("200 OK\n", outputStreamCaptor.toString());
    }

    @Test
    void testPrintResponseBodyAsMap() {
        Map<String, Object> body = new HashMap<>();
        body.put("key1", "value1");
        body.put("key2", "value2");
        HttpResponse<Map<String, Object>> response = new HttpResponse<>(ResponseCode.SUCCESS.getMessage(), ResponseCode.SUCCESS, body);

        printHandler.printResponseBodyAsMap(response);

        assertEquals("key1 : value1\nkey2 : value2\n", outputStreamCaptor.toString());
    }

    @Test
    void testPrintResponseBodyAsMapList() {
        List<Map<String, Object>> body = Arrays.asList(
                Map.of("key1", "value1", "key2", "value2"),
                Map.of("key3", "value3", "key4", "value4")
        );
        HttpResponse<List<Map<String, Object>>> response = new HttpResponse<>(ResponseCode.UNAUTHORIZED.getMessage(), ResponseCode.SUCCESS, body);

        printHandler.printResponseBodyAsMapList(response);

        String output = outputStreamCaptor.toString();
        long lineCount = output.lines().count();

        assertEquals(body.size(), lineCount);
    }

    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
    }

}
