package controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import config.annotations.Controller;
import config.annotations.Requires;
import controller.annotations.DeleteMapping;
import controller.annotations.GetMapping;
import controller.annotations.PostMapping;
import controller.annotations.PutMapping;
import domain.Member;
import dto.MemberLoginDTO;
import dto.MemberRegisterDTO;
import dto.MemberUpdateDTO;
import exception.MemberNotFoundException;
import http.request.HttpRequest;
import http.response.HttpResponse;
import http.response.ResponseCode;
import javassist.NotFoundException;
import message.ExceptionMessage;
import message.PrintResultMessage;

import service.MemberAuthService;
import service.MemberService;
import view.PrintHandler;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Controller
@Requires(dependsOn = {MemberService.class, MemberAuthService.class, PrintHandler.class})
public class MemberController implements ControllerInterface {

    private final MemberService memberService;
    private final MemberAuthService memberAuthService;
    private final PrintHandler printHandler;

    public MemberController(MemberService memberService, MemberAuthService memberAuthService, PrintHandler printHandler) {
        this.memberService = memberService;
        this.memberAuthService = memberAuthService;
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

    @GetMapping(path = "/accounts/detail")
    public void detail(HttpRequest<Map<String, Object>> request) throws RuntimeException {
        String id = request.getQueryParams().get("accountId");
        Optional<Member> memberById = memberService.getMemberById(id);
        if (memberById.isPresent()) {
            Map<String, Object> responseBody = new HashMap<>();
            responseBody.put("계정", id);
            responseBody.put("회원", memberById.get().getName());
            responseBody.put("이메일", memberById.get().getEmail());
            responseBody.put("가입일", memberById.get().getJoinDate());

            HttpResponse<Map<String, Object>> response = new HttpResponse<>(
                    ResponseCode.SUCCESS.getMessage(),
                    ResponseCode.SUCCESS,
                    responseBody
            );

            printHandler.printResponseBodyAsMap(response);
            return;
        }
        throw new MemberNotFoundException(ExceptionMessage.MEMBER_NOT_FOUND);
    }

    @PutMapping(path = "/accounts/edit")
    public void edit(HttpRequest<Map<String, Object>> request) throws RuntimeException {
        String id = request.getQueryParams().get("accountId");
        Map<String, Object> body = request.getBody();
        MemberUpdateDTO memberUpdateDTO = new MemberUpdateDTO(
                (String) body.get("email"),
                (String) body.get("password")
        );
        memberService.updateMember(id, memberUpdateDTO);
        HttpResponse<?> response = new HttpResponse<>(
                PrintResultMessage.ACCOUNTS_MEMBER_EDIT,
                ResponseCode.SUCCESS,
                null
        );
        printHandler.printSuccessWithResponseCodeAndCustomMessage(response);
    }

    @DeleteMapping(path = "/accounts/remove")
    public void remove(HttpRequest<Map<String, Object>> request) {
        String id = request.getQueryParams().get("accountId");
        memberAuthService.logout();
        memberService.deleteMember(id);
        HttpResponse<?> response = new HttpResponse<>(
                PrintResultMessage.ACCOUNTS_DELETE_SUCCESS,
                ResponseCode.SUCCESS,
                null
        );
        printHandler.printSuccessWithResponseCodeAndCustomMessage(response);
    }

    @GetMapping(path = "/accounts/testadmin")
    public void createTestAdmin() {
        memberService.registerAdminMember();
    }

}
