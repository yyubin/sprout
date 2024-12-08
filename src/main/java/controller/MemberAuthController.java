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
@Requires(dependsOn = {MemberAuthServiceInterface.class})
public class MemberAuthController implements ControllerInterface{

    private final MemberAuthServiceInterface memberAuthService;

    public MemberAuthController(MemberAuthServiceInterface memberAuthService) {
        this.memberAuthService = memberAuthService;
    }

    @PostMapping(path = "/accounts/signin")
    public HttpResponse<?> signin(MemberLoginDTO memberLoginDTO) throws Throwable {
        String sessionId = memberAuthService.login(memberLoginDTO);
        return new HttpResponse<>(
                PrintResultMessage.ACCOUNTS_LOGIN_SUCCESS + sessionId,
                ResponseCode.SUCCESS,
                null
        );

    }

    @GetMapping(path = "/accounts/signout")
    public HttpResponse<?> signout() throws Throwable {
        memberAuthService.logout();
        return new HttpResponse<>(
                PrintResultMessage.ACCOUNTS_LOGOUT_SUCCESS,
                ResponseCode.SUCCESS,
                null
        );

    }

}
