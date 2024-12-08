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
import service.interfaces.MemberAuthServiceInterface;
import service.interfaces.MemberServiceInterface;
import view.PrintHandler;
import view.interfaces.PrintProcessor;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Controller
@Requires(dependsOn = {MemberServiceInterface.class, MemberAuthServiceInterface.class})
public class MemberController implements ControllerInterface {

    private final MemberServiceInterface memberService;
    private final MemberAuthServiceInterface memberAuthService;

    public MemberController(MemberServiceInterface memberService, MemberAuthServiceInterface memberAuthService) {
        this.memberService = memberService;
        this.memberAuthService = memberAuthService;
    }

    @PostMapping(path = "/accounts/signup")
    public HttpResponse<?> signup(MemberRegisterDTO memberRegisterDTO) throws RuntimeException {
        System.out.println("MemberController.signup");
        memberService.registerMember(memberRegisterDTO);
        return new HttpResponse<>(
                PrintResultMessage.ACCOUNTS_SIGNUP_SUCCESS,
                ResponseCode.CREATED,
                null
        );
    }

    @GetMapping(path = "/accounts/detail")
    public HttpResponse<?> detail(String accountId) throws RuntimeException {
        Optional<Member> memberById = memberService.getMemberById(accountId);

        if (memberById.isPresent()) {
            Map<String, Object> responseBody = new HashMap<>();
            responseBody.put("계정", accountId);
            responseBody.put("회원", memberById.get().getName());
            responseBody.put("이메일", memberById.get().getEmail());
            responseBody.put("가입일", memberById.get().getJoinDate());

             return new HttpResponse<>(
                    ResponseCode.SUCCESS.getMessage(),
                    ResponseCode.SUCCESS,
                    responseBody
            );
        }
        throw new MemberNotFoundException(ExceptionMessage.MEMBER_NOT_FOUND);
    }

    @PutMapping(path = "/accounts/edit")
    public HttpResponse<?> edit(String accountId, MemberUpdateDTO memberUpdateDTO) throws RuntimeException {
        memberService.updateMember(accountId, memberUpdateDTO);
        return new HttpResponse<>(
                PrintResultMessage.ACCOUNTS_MEMBER_EDIT,
                ResponseCode.ACCEPT,
                null
        );

    }

    @DeleteMapping(path = "/accounts/remove")
    public HttpResponse<?> remove(String accountId) throws Throwable {
        memberAuthService.logout();
        memberService.deleteMember(accountId);
        return new HttpResponse<>(
                PrintResultMessage.ACCOUNTS_DELETE_SUCCESS,
                ResponseCode.ACCEPT,
                null
        );

    }

    @GetMapping(path = "/accounts/testadmin")
    public HttpResponse<?> createTestAdmin() {
        memberService.registerAdminMember();
        return new HttpResponse<>(
                "AMDIN created",
                ResponseCode.CREATED,
                null
        );
    }

}
