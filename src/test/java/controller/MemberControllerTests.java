package controller;

import domain.Member;
import dto.MemberRegisterDTO;
import dto.MemberUpdateDTO;
import http.request.HttpRequest;
import http.response.HttpResponse;
import org.junit.Before;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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

import static org.mockito.Mockito.*;

public class MemberControllerTests {

    private MemberServiceInterface memberService;
    private MemberAuthServiceInterface memberAuthService;
    private PrintProcessor printHandler;
    private MemberController memberController;

    @BeforeEach
    public void setUp() {
        memberService = mock(MemberService.class);
        memberAuthService = mock(MemberAuthService.class);
        printHandler = mock(PrintHandler.class);
        memberController = new MemberController(memberService, memberAuthService, printHandler);
    }

    @Test
    void testSignup() {
        MemberRegisterDTO memberRegisterDTO = new MemberRegisterDTO("testUser", "Test User", "test@example.com", "password123");

        @SuppressWarnings("unchecked")
        HttpRequest<MemberRegisterDTO> request = mock(HttpRequest.class);
        when(request.getBody()).thenReturn(memberRegisterDTO);

        memberController.signup(memberRegisterDTO);

        verify(memberService, times(1)).registerMember(any(MemberRegisterDTO.class));
        verify(printHandler, times(1)).printSuccessWithResponseCodeAndCustomMessage(any(HttpResponse.class));
    }

    @Test
    void testDetailMemberFound() {
        // Prepare request and mock behavior
        String accountId = "testUser";
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("accountId", accountId);

        @SuppressWarnings("unchecked")
        HttpRequest<Map<String, String>> request = mock(HttpRequest.class);
        when(request.getQueryParams()).thenReturn(queryParams);

        Member mockMember = new Member(accountId, "Test User", "test@example.com", LocalDate.now(), "qwer");
        when(memberService.getMemberById(accountId)).thenReturn(Optional.of(mockMember));

        memberController.detail(accountId);

        verify(printHandler, times(1)).printResponseBodyAsMap(any(HttpResponse.class));
    }

    @Test
    void testEditMember() {
        String accountId = "testUser";
        MemberUpdateDTO memberUpdateDTO = new MemberUpdateDTO("updated@example.com", "newPassword123");

        @SuppressWarnings("unchecked")
        HttpRequest<MemberUpdateDTO> request = mock(HttpRequest.class);
        when(request.getQueryParams()).thenReturn(Map.of("accountId", accountId));
        when(request.getBody()).thenReturn(memberUpdateDTO);

        memberController.edit(accountId, memberUpdateDTO);

        verify(memberService, times(1)).updateMember(eq(accountId), any(MemberUpdateDTO.class));
        verify(printHandler, times(1)).printSuccessWithResponseCodeAndCustomMessage(any(HttpResponse.class));
    }

    @Test
    void testRemoveMember() throws Throwable {
        String accountId = "testUser";

        @SuppressWarnings("unchecked")
        HttpRequest<Map<String, String>> request = mock(HttpRequest.class);
        when(request.getQueryParams()).thenReturn(Map.of("accountId", accountId));

        memberController.remove(accountId);

        verify(memberAuthService, times(1)).logout();
        verify(memberService, times(1)).deleteMember(accountId);
        verify(printHandler, times(1)).printSuccessWithResponseCodeAndCustomMessage(any(HttpResponse.class));
    }

    @Test
    void testCreateTestAdmin() {
        memberController.createTestAdmin();
        verify(memberService, times(1)).registerAdminMember();
    }
}
