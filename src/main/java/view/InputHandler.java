package view;

import config.annotations.Component;
import config.annotations.Requires;
import http.request.RequestHandler;
import message.ExceptionMessage;
import message.InputCautionMessage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Scanner;

@Component
@Requires(dependsOn = {RequestHandler.class})
public class InputHandler {

    private final RequestHandler requestHandler;

    public InputHandler(RequestHandler requestHandler) {
        this.requestHandler = requestHandler;
    }

    public void startInputLoop() {
        System.out.println(InputCautionMessage.EXIT_GUIDE_MESSAGE);
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        StringBuilder rawRequest = new StringBuilder();
        String line;

        try {
            while (true) {
                System.out.println(InputCautionMessage.CAUTION_COMMAND_FOR_HTTP);
                rawRequest.setLength(0);

                while (!(line = reader.readLine()).isEmpty()){
                    rawRequest.append(line).append("\n");
                }

                if (rawRequest.toString().trim().isEmpty()){
                    break;
                }
                requestHandler.handleRequest(rawRequest.toString());
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
