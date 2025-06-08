package controller;

import app.controller.MemberAuthController;
import app.dto.MemberLoginDTO;
import sprout.mvc.http.HttpResponse;
import sprout.mvc.http.ResponseCode;
import message.PrintResultMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import app.service.MemberAuthService;
import app.service.interfaces.MemberAuthServiceInterface;
import view.PrintHandler;
import view.interfaces.PrintProcessor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class MemberAuthControllerTests {

    private MemberAuthServiceInterface mockMemberAuthService;
    private PrintProcessor mockPrintHandler;
    private MemberAuthController memberAuthController;

    @BeforeEach
    void setUp() {
        mockMemberAuthService = mock(MemberAuthService.class);
        mockPrintHandler = mock(PrintHandler.class);
        memberAuthController = new MemberAuthController(mockMemberAuthService);
    }

    @Test
    void testSignin() throws Throwable {
        String sessionId = "mockSessionId";
        MemberLoginDTO memberLoginDTO = new MemberLoginDTO("testUser", "password123");

        when(mockMemberAuthService.login(any(MemberLoginDTO.class))).thenReturn(sessionId);

        memberAuthController.signin(memberLoginDTO);

        verify(mockMemberAuthService, times(1)).login(any(MemberLoginDTO.class));
        verify(mockPrintHandler, times(1)).printSuccessWithResponseCodeAndCustomMessage(any(HttpResponse.class));

        ArgumentCaptor<HttpResponse<?>> responseCaptor = ArgumentCaptor.forClass(HttpResponse.class);
        verify(mockPrintHandler).printSuccessWithResponseCodeAndCustomMessage(responseCaptor.capture());

        HttpResponse<?> response = responseCaptor.getValue();
        assertEquals(PrintResultMessage.ACCOUNTS_LOGIN_SUCCESS + sessionId, response.getDescription());
        assertEquals(ResponseCode.SUCCESS, response.getResponseCode());
    }

    @Test
    void testSignout() throws Throwable {
        memberAuthController.signout();

        verify(mockMemberAuthService, times(1)).logout();
        verify(mockPrintHandler, times(1)).printSuccessWithResponseCodeAndCustomMessage(any(HttpResponse.class));

        ArgumentCaptor<HttpResponse<?>> responseCaptor = ArgumentCaptor.forClass(HttpResponse.class);
        verify(mockPrintHandler).printSuccessWithResponseCodeAndCustomMessage(responseCaptor.capture());

        HttpResponse<?> response = responseCaptor.getValue();
        assertEquals(PrintResultMessage.ACCOUNTS_LOGOUT_SUCCESS, response.getDescription());
        assertEquals(ResponseCode.SUCCESS, response.getResponseCode());
    }

}
