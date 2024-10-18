package controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import config.annotations.Controller;
import config.annotations.Requires;
import controller.annotations.DeleteMapping;
import controller.annotations.GetMapping;
import controller.annotations.PostMapping;
import controller.annotations.PutMapping;
import dto.MemberRegisterDTO;
import http.request.HttpRequest;
import http.response.HttpResponse;
import http.response.ResponseCode;
import message.PrintResultMessage;

import service.MemberService;
import view.PrintHandler;

import java.time.LocalDate;
import java.util.Map;

@Controller
@Requires(dependsOn = {MemberService.class, PrintHandler.class})
public class MemberController implements ControllerInterface {

    private final MemberService memberService;
    private final PrintHandler printHandler;

    public MemberController(MemberService memberService, PrintHandler printHandler) {
        this.memberService = memberService;
        this.printHandler = printHandler;
    }

    @PostMapping(path = "/accounts/signup")
    public void signup(HttpRequest<Map<String, Object>> request) throws RuntimeException {
        Map<String, Object> body = request.getBody();
        MemberRegisterDTO memberRegisterDTO = new MemberRegisterDTO(
                (String) body.get("id"),
                (String) body.get("name"),
                (String) body.get("email"),
                (String) body.get("password"),
                LocalDate.now()
        );
        memberService.registerMember(memberRegisterDTO);
        HttpResponse<?> response = new HttpResponse<>(
                PrintResultMessage.ACCOUNTS_SIGNUP_SUCCESS,
                ResponseCode.SUCCESS,
                null
        );
        printHandler.printSuccessWithResponseCodeAndCustomMessage(response);
    }

    @PostMapping(path = "/accounts/signin")
    public void signin() {

    }

    @GetMapping(path = "/accounts/signout")
    public void signout() {

    }

    @GetMapping(path = "/accounts/detail")
    public void detail() {

    }

    @PutMapping(path = "/accounts/edit")
    public void edit() {

    }

    @DeleteMapping(path = "/accounts/remove")
    public void remove() {

    }

}
