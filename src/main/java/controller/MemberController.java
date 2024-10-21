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
    public void signup(MemberRegisterDTO memberRegisterDTO) throws RuntimeException {
        memberService.registerMember(memberRegisterDTO);
        HttpResponse<?> response = new HttpResponse<>(
                PrintResultMessage.ACCOUNTS_SIGNUP_SUCCESS,
                ResponseCode.SUCCESS,
                null
        );
        printHandler.printSuccessWithResponseCodeAndCustomMessage(response);
    }

    @GetMapping(path = "/accounts/detail")
    public void detail(String accountId) throws RuntimeException {
        Optional<Member> memberById = memberService.getMemberById(accountId);
        if (memberById.isPresent()) {
            Map<String, Object> responseBody = new HashMap<>();
            responseBody.put("계정", accountId);
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
    public void edit(String accountId, MemberUpdateDTO memberUpdateDTO) throws RuntimeException {
        memberService.updateMember(accountId, memberUpdateDTO);
        HttpResponse<?> response = new HttpResponse<>(
                PrintResultMessage.ACCOUNTS_MEMBER_EDIT,
                ResponseCode.SUCCESS,
                null
        );
        printHandler.printSuccessWithResponseCodeAndCustomMessage(response);
    }

    @DeleteMapping(path = "/accounts/remove")
    public void remove(String accountId) throws Throwable {
        memberAuthService.logout();
        memberService.deleteMember(accountId);
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
