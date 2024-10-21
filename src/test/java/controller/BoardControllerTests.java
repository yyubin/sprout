package controller;

import domain.Board;
import domain.Member;
import domain.Post;
import domain.grade.MemberGrade;
import dto.BoardRegisterDTO;
import dto.BoardUpdateDTO;
import http.request.HttpRequest;
import http.response.HttpResponse;
import http.response.ResponseCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import service.BoardService;
import service.PostService;
import view.PrintHandler;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

public class BoardControllerTests {

    private BoardService mockBoardService;
    private PostService mockPostService;
    private PrintHandler mockPrintHandler;
    private BoardController boardController;

    @BeforeEach
    void setUp() {
        mockBoardService = mock(BoardService.class);
        mockPostService = mock(PostService.class);
        mockPrintHandler = mock(PrintHandler.class);
        boardController = new BoardController(mockBoardService, mockPostService, mockPrintHandler);
    }

    @Test
    void testAddBoard() throws Throwable {
        Map<String, Object> body = new HashMap<>();
        body.put("boardName", "Test Board");
        body.put("description", "Test Board Description");
        body.put("grades", MemberGrade.USER.getDescription());

        HttpRequest<Map<String, Object>> request = mock(HttpRequest.class);
        when(request.getBody()).thenReturn(body);

        boardController.addBoard(request);

        ArgumentCaptor<BoardRegisterDTO> dtoCaptor = ArgumentCaptor.forClass(BoardRegisterDTO.class);
        verify(mockBoardService, times(1)).createBoard(dtoCaptor.capture());

        BoardRegisterDTO capturedDto = dtoCaptor.getValue();
        assertEquals("Test Board", capturedDto.getBoardName());
        assertEquals("Test Board Description", capturedDto.getDescription());
        assertTrue(capturedDto.getAccessGrades().contains(MemberGrade.ADMIN));
        assertTrue(capturedDto.getAccessGrades().contains(MemberGrade.USER));

        verify(mockPrintHandler, times(1)).printSuccessWithResponseCodeAndCustomMessage(any(HttpResponse.class));
    }

    @Test
    void testEditBoard() throws Throwable {
        Map<String, Object> body = new HashMap<>();
        body.put("boardName", "Updated Board");
        body.put("description", "Updated Description");
        body.put("grades", MemberGrade.USER.getDescription());

        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("boardId", "1");

        HttpRequest<Map<String, Object>> request = mock(HttpRequest.class);
        when(request.getBody()).thenReturn(body);
        when(request.getQueryParams()).thenReturn(queryParams);

        boardController.editBoard(request);

        ArgumentCaptor<BoardUpdateDTO> dtoCaptor = ArgumentCaptor.forClass(BoardUpdateDTO.class);
        verify(mockBoardService, times(1)).updateBoard(dtoCaptor.capture());

        BoardUpdateDTO capturedDto = dtoCaptor.getValue();
        assertEquals(1L, capturedDto.getId());
        assertEquals("Updated Board", capturedDto.getBoardName());
        assertEquals("Updated Description", capturedDto.getDescription());
        assertTrue(capturedDto.getAccessGrades().contains(MemberGrade.ADMIN));
        assertTrue(capturedDto.getAccessGrades().contains(MemberGrade.USER));

        verify(mockPrintHandler, times(1)).printSuccessWithResponseCodeAndCustomMessage(any(HttpResponse.class));
    }

    @Test
    void testRemoveBoard() throws Throwable {
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("boardId", "1");

        HttpRequest<Map<String, Object>> request = mock(HttpRequest.class);
        when(request.getQueryParams()).thenReturn(queryParams);

        boardController.removeBoard(request);

        verify(mockBoardService, times(1)).deleteBoard(1L);
        verify(mockPrintHandler, times(1)).printSuccessWithResponseCodeAndCustomMessage(any(HttpResponse.class));
    }

    @Test
    void testViewBoard() throws Exception {
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("boardName", "Test Board");

        HttpRequest<Map<String, Object>> request = mock(HttpRequest.class);
        when(request.getQueryParams()).thenReturn(queryParams);

        Member mockAuthor = mock(Member.class);
        Board mockBoard = mock(Board.class);
        when(mockBoard.generatedPostId()).thenReturn(1L);

        List<Post> mockPosts = List.of(
                new Post("Test Post 1", "Content for Test Post 1", mockAuthor, mockBoard, LocalDateTime.now()),
                new Post("Test Post 2", "Content for Test Post 2", mockAuthor, mockBoard, LocalDateTime.now())
        );
        when(mockPostService.getPostsByBoardName("Test Board")).thenReturn(mockPosts);

        boardController.viewBoard(request);

        verify(mockPostService, times(1)).getPostsByBoardName("Test Board");

        ArgumentCaptor<HttpResponse<List<Map<String, Object>>>> responseCaptor = ArgumentCaptor.forClass(HttpResponse.class);
        verify(mockPrintHandler, times(1)).printResponseBodyAsMapList(responseCaptor.capture());

        HttpResponse<List<Map<String, Object>>> response = responseCaptor.getValue();
        assertEquals(ResponseCode.SUCCESS.getMessage(), response.getDescription());
        assertEquals(ResponseCode.SUCCESS, response.getResponseCode());

        List<Map<String, Object>> postSummaryList = response.getBody();
        assertEquals(2, postSummaryList.size());
        assertEquals(1L, postSummaryList.getFirst().get("게시글 번호"));
        assertEquals("Test Post 1", postSummaryList.getFirst().get("게시글 이름"));
    }

}
