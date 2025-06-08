package app.controller;

import sprout.beans.annotation.Controller;
import sprout.beans.annotation.Requires;
import sprout.mvc.annotation.GetMapping;
import sprout.mvc.annotation.PostMapping;
import app.dto.MemberLoginDTO;
import sprout.mvc.http.HttpResponse;
import sprout.mvc.http.ResponseCode;
import app.message.PrintResultMessage;
import app.service.interfaces.MemberAuthServiceInterface;
import sprout.mvc.mapping.ControllerInterface;

@Controller
@Requires(dependsOn = {MemberAuthServiceInterface.class})
public class MemberAuthController implements ControllerInterface {

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
