package controller;

import config.annotations.Controller;
import config.annotations.Requires;
import controller.annotations.DeleteMapping;
import controller.annotations.GetMapping;
import controller.annotations.PostMapping;
import controller.annotations.PutMapping;
import domain.Board;
import domain.Post;
import domain.grade.MemberGrade;
import dto.BoardRegisterDTO;
import dto.BoardUpdateDTO;
import http.request.HttpRequest;
import http.response.HttpResponse;
import http.response.ResponseCode;
import message.PrintResultMessage;
import service.BoardService;
import service.PostService;
import service.interfaces.BoardServiceInterface;
import service.interfaces.PostServiceInterface;
import view.PrintHandler;
import view.interfaces.PrintProcessor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@Requires(dependsOn = {BoardServiceInterface.class, PostServiceInterface.class, PrintProcessor.class})
public class BoardController implements ControllerInterface{

    private final BoardServiceInterface boardService;
    private final PostServiceInterface postService;
    private final PrintProcessor printHandler;

    public BoardController(BoardServiceInterface boardService, PostServiceInterface postService, PrintProcessor printHandler) {
        this.boardService = boardService;
        this.postService = postService;
        this.printHandler = printHandler;
    }

    @PostMapping(path = "/boards/add")
    public void addBoard(BoardRegisterDTO boardRegisterDTO) throws Throwable {
        boardService.createBoard(boardRegisterDTO);

        HttpResponse<?> response = new HttpResponse<>(
                PrintResultMessage.BOARD_CREATE_SUCCESS,
                ResponseCode.SUCCESS,
                null
        );
        printHandler.printSuccessWithResponseCodeAndCustomMessage(response);
    }

    @PutMapping(path = "/boards/edit")
    public void editBoard(Long boardId, BoardUpdateDTO boardUpdateDTO) throws Throwable {
        boardService.updateBoard(boardId, boardUpdateDTO);

        HttpResponse<?> response = new HttpResponse<>(
                PrintResultMessage.BOARD_UPDATE_SUCCESS,
                ResponseCode.SUCCESS,
                null
        );
        printHandler.printSuccessWithResponseCodeAndCustomMessage(response);
    }

    @DeleteMapping(path = "/boards/remove")
    public void removeBoard(Long boardId) throws Throwable {
        boardService.deleteBoard(boardId);
        HttpResponse<?> response = new HttpResponse<>(
                PrintResultMessage.BOARD_DELETE_SUCCESS,
                ResponseCode.SUCCESS,
                null
        );
        printHandler.printSuccessWithResponseCodeAndCustomMessage(response);
    }

    @GetMapping(path = "/boards/view")
    public void viewBoard(String boardName) throws Exception {
        List<Post> postList = postService.getPostsByBoardName(boardName);

        List<Map<String, Object>> postSummaryList = postList.stream()
                .map(post -> {
                    Map<String, Object> postSummary = new HashMap<>();
                    postSummary.put("게시글 번호", post.getPostId());
                    postSummary.put("게시글 이름", post.getPostName());
                    postSummary.put("작성일", post.getCreatedDate());
                    return postSummary;
                })
                .toList();

        HttpResponse<List<Map<String, Object>>> response = new HttpResponse<>(
                ResponseCode.SUCCESS.getMessage(),
                ResponseCode.SUCCESS,
                postSummaryList
        );
        printHandler.printResponseBodyAsMapList(response);
    }

}
