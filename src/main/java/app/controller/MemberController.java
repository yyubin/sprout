package app.controller;

import sprout.beans.annotation.Controller;
import sprout.beans.annotation.Requires;
import sprout.mvc.annotation.DeleteMapping;
import sprout.mvc.annotation.GetMapping;
import sprout.mvc.annotation.PostMapping;
import sprout.mvc.annotation.PutMapping;
import app.domain.Member;
import app.dto.MemberRegisterDTO;
import app.dto.MemberUpdateDTO;
import app.exception.MemberNotFoundException;
import sprout.mvc.http.HttpResponse;
import sprout.mvc.http.ResponseCode;
import app.message.ExceptionMessage;
import app.message.PrintResultMessage;

import app.service.interfaces.MemberAuthServiceInterface;
import app.service.interfaces.MemberServiceInterface;
import sprout.mvc.mapping.ControllerInterface;

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
