package controller;

import config.annotations.Controller;
import config.annotations.Requires;
import controller.annotations.GetMapping;
import controller.annotations.PostMapping;
import dto.MemberLoginDTO;
import http.request.HttpRequest;
import http.response.HttpResponse;
import http.response.ResponseCode;
import message.PrintResultMessage;
import service.MemberAuthService;
import service.interfaces.MemberAuthServiceInterface;
import view.PrintHandler;
import view.interfaces.PrintProcessor;

import java.util.Map;

@Controller
@Requires(dependsOn = {MemberAuthServiceInterface.class, PrintProcessor.class})
public class MemberAuthController implements ControllerInterface{

    private final MemberAuthServiceInterface memberAuthService;
    private final PrintProcessor printHandler;

    public MemberAuthController(MemberAuthServiceInterface memberAuthService, PrintProcessor printHandler) {
        this.memberAuthService = memberAuthService;
        this.printHandler = printHandler;
    }

    @PostMapping(path = "/accounts/signin")
    public void signin(MemberLoginDTO memberLoginDTO) throws Throwable {
        String sessionId = memberAuthService.login(memberLoginDTO);
        HttpResponse<?> response = new HttpResponse<>(
                PrintResultMessage.ACCOUNTS_LOGIN_SUCCESS + sessionId,
                ResponseCode.SUCCESS,
                null
        );
        printHandler.printSuccessWithResponseCodeAndCustomMessage(response);
    }

    @GetMapping(path = "/accounts/signout")
    public void signout() throws Throwable {
        memberAuthService.logout();
        HttpResponse<?> response = new HttpResponse<>(
                PrintResultMessage.ACCOUNTS_LOGOUT_SUCCESS,
                ResponseCode.SUCCESS,
                null
        );
        printHandler.printSuccessWithResponseCodeAndCustomMessage(response);
    }

}
