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
@Requires(dependsOn = {BoardServiceInterface.class, PostServiceInterface.class})
public class BoardController implements ControllerInterface{

    private final BoardServiceInterface boardService;
    private final PostServiceInterface postService;

    public BoardController(BoardServiceInterface boardService, PostServiceInterface postService) {
        this.boardService = boardService;
        this.postService = postService;
    }

    @PostMapping(path = "/boards/add")
    public HttpResponse<?> addBoard(BoardRegisterDTO boardRegisterDTO) throws Throwable {
        try {
            boardService.createBoard(boardRegisterDTO);
        } catch (Throwable e) {
            throw new Throwable(e.getMessage());
        }

        return new HttpResponse<>(
                PrintResultMessage.BOARD_CREATE_SUCCESS,
                ResponseCode.CREATED,
                null
        );

    }

    @PutMapping(path = "/boards/edit")
    public HttpResponse<?> editBoard(Long boardId, BoardUpdateDTO boardUpdateDTO) throws Throwable {
        try {
            boardService.updateBoard(boardId, boardUpdateDTO);
        } catch (Throwable e) {
            throw new Throwable(e.getMessage());
        }

        return new HttpResponse<>(
                PrintResultMessage.BOARD_UPDATE_SUCCESS,
                ResponseCode.ACCEPT,
                null
        );
    }

    @DeleteMapping(path = "/boards/remove")
    public HttpResponse<?> removeBoard(Long boardId) throws Throwable {
        try {
            boardService.deleteBoard(boardId);
        } catch (Throwable e) {
            throw new Throwable(e.getMessage());
        }

        return new HttpResponse<>(
                PrintResultMessage.BOARD_DELETE_SUCCESS,
                ResponseCode.ACCEPT,
                null
        );

    }

    @GetMapping(path = "/boards/view")
    public HttpResponse<?> viewBoard(String boardName) throws Exception {
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

        return new HttpResponse<>(
                ResponseCode.SUCCESS.getMessage(),
                ResponseCode.SUCCESS,
                postSummaryList
        );

    }

}
