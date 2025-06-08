package controller;

import app.controller.BoardController;
import app.domain.grade.MemberGrade;
import app.dto.BoardRegisterDTO;
import app.dto.BoardUpdateDTO;
import sprout.mvc.http.HttpResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import app.service.BoardService;
import app.service.PostService;
import app.service.interfaces.BoardServiceInterface;
import app.service.interfaces.PostServiceInterface;
import view.PrintHandler;
import view.interfaces.PrintProcessor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

public class BoardControllerTests {

    private BoardServiceInterface mockBoardService;
    private PostServiceInterface mockPostService;
    private PrintProcessor mockPrintHandler;
    private BoardController boardController;

    @BeforeEach
    void setUp() {
        mockBoardService = mock(BoardService.class);
        mockPostService = mock(PostService.class);
        mockPrintHandler = mock(PrintHandler.class);
        boardController = new BoardController(mockBoardService, mockPostService);
    }

    @Test
    void testAddBoard() throws Throwable {
        BoardRegisterDTO boardRegisterDTO = new BoardRegisterDTO("Test Board", "Test Board Description", MemberGrade.USER.getDescription());

        boardController.addBoard(boardRegisterDTO);

        ArgumentCaptor<BoardRegisterDTO> dtoCaptor = ArgumentCaptor.forClass(BoardRegisterDTO.class);
        verify(mockBoardService, times(1)).createBoard(dtoCaptor.capture());

        BoardRegisterDTO capturedDto = dtoCaptor.getValue();
        assertEquals("Test Board", capturedDto.getBoardName());
        assertEquals("Test Board Description", capturedDto.getDescription());

        verify(mockPrintHandler, times(1)).printSuccessWithResponseCodeAndCustomMessage(any(HttpResponse.class));
    }

    @Test
    void testEditBoard() throws Throwable {
        Long boardId = 1L;
        BoardUpdateDTO boardUpdateDTO = new BoardUpdateDTO(boardId, "Updated Board", "Updated Description", MemberGrade.USER.getDescription());

        boardController.editBoard(boardId, boardUpdateDTO);

        ArgumentCaptor<BoardUpdateDTO> dtoCaptor = ArgumentCaptor.forClass(BoardUpdateDTO.class);
        verify(mockBoardService, times(1)).updateBoard(eq(boardId), dtoCaptor.capture());

        BoardUpdateDTO capturedDto = dtoCaptor.getValue();
        assertEquals(boardId, capturedDto.getId());
        assertEquals("Updated Board", capturedDto.getBoardName());
        assertEquals("Updated Description", capturedDto.getDescription());

        verify(mockPrintHandler, times(1)).printSuccessWithResponseCodeAndCustomMessage(any(HttpResponse.class));
    }

    @Test
    void testRemoveBoard() throws Throwable {
        Long boardId = 1L;

        boardController.removeBoard(boardId);

        verify(mockBoardService, times(1)).deleteBoard(boardId);
        verify(mockPrintHandler, times(1)).printSuccessWithResponseCodeAndCustomMessage(any(HttpResponse.class));
    }

//    @Test
//    void testViewBoard() throws Exception {
//        String boardName = "Test Board";
//        Member mockAuthor = mock(Member.class);
//        Board mockBoard = mock(Board.class);
//        when(mockBoard.generatedPostId()).thenReturn(1L);
//
//        List<Post> mockPosts = List.of(
//                new Post("Test Post 1", "Content for Test Post 1", mockAuthor, mockBoard, LocalDateTime.now()),
//                new Post("Test Post 2", "Content for Test Post 2", mockAuthor, mockBoard, LocalDateTime.now())
//        );
//        when(mockPostService.getPostsByBoardName(boardName)).thenReturn(mockPosts);
//
//        boardController.viewBoard(boardName);
//
//        verify(mockPostService, times(1)).getPostsByBoardName(boardName);
//
//        ArgumentCaptor<HttpResponse<List<Map<String, Object>>>> responseCaptor = ArgumentCaptor.forClass(HttpResponse.class);
//        verify(mockPrintHandler, times(1)).printResponseBodyAsMapList(responseCaptor.capture());
//
//        HttpResponse<List<Map<String, Object>>> response = responseCaptor.getValue();
//        assertEquals(ResponseCode.SUCCESS.getMessage(), response.getDescription());
//        assertEquals(ResponseCode.SUCCESS, response.getResponseCode());
//
//        List<Map<String, Object>> postSummaryList = response.getBody();
//        assertEquals(2, postSummaryList.size());
//        assertEquals(1L, postSummaryList.getFirst().get("게시글 번호"));
//        assertEquals("Test Post 1", postSummaryList.getFirst().get("게시글 이름"));
//    }

}
