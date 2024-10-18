import com.fasterxml.jackson.core.JsonProcessingException;
import config.Container;
import controller.ExampleController;
import exception.BadRequestException;
import http.request.HttpRequest;
import http.request.HttpRequestParser;
import http.request.RequestHandler;
import repository.InMemoryMemberRepository;
import service.MemberService;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class Main {

    public static void main(String[] args) throws Exception {

        Container container = new Container();
        container.scan("repository");
        container.scan("util");
        container.scan("service");


//        InMemoryMemberRepository memberRepository = container.get(InMemoryMemberRepository.class);
//        MemberService memberService = new MemberService(memberRepository);
//
//        container.register(MemberService.class, memberService);



//        System.out.println("HTTP Request 형식으로 입력해주세요: ");
//
//        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
//        StringBuilder rawRequest = new StringBuilder();
//        String line;
//
//        while (!(line = reader.readLine()).isEmpty()) {
//            rawRequest.append(line).append("\n");
//        }
//
//        RequestHandler handler = new RequestHandler(new ExampleController());
//        handler.handleRequest(rawRequest.toString());
    }

}
