//package app.controller;
//
//import sprout.beans.annotation.Controller;
//import sprout.beans.annotation.Requires;
//import sprout.mvc.annotation.DeleteMapping;
//import sprout.mvc.annotation.GetMapping;
//import sprout.mvc.annotation.PostMapping;
//import sprout.mvc.annotation.PutMapping;
//import app.domain.Post;
//import app.dto.BoardRegisterDTO;
//import app.dto.BoardUpdateDTO;
//import sprout.mvc.http.HttpResponse;
//import sprout.mvc.http.ResponseCode;
//import app.message.PrintResultMessage;
//import app.service.interfaces.BoardServiceInterface;
//import app.service.interfaces.PostServiceInterface;
//import sprout.mvc.mapping.ControllerInterface;
//
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//
//@Controller
//@Requires(dependsOn = {BoardServiceInterface.class, PostServiceInterface.class})
//public class BoardController implements ControllerInterface {
//
//    private final BoardServiceInterface boardService;
//    private final PostServiceInterface postService;
//
//    public BoardController(BoardServiceInterface boardService, PostServiceInterface postService) {
//        this.boardService = boardService;
//        this.postService = postService;
//    }
//
//    @PostMapping(path = "/boards/add")
//    public HttpResponse<?> addBoard(BoardRegisterDTO boardRegisterDTO) throws Throwable {
//        try {
//            boardService.createBoard(boardRegisterDTO);
//        } catch (Throwable e) {
//            throw new Throwable(e.getMessage());
//        }
//
//        return new HttpResponse<>(
//                PrintResultMessage.BOARD_CREATE_SUCCESS,
//                ResponseCode.CREATED,
//                null
//        );
//
//    }
//
//    @PutMapping(path = "/boards/edit")
//    public HttpResponse<?> editBoard(Long boardId, BoardUpdateDTO boardUpdateDTO) throws Throwable {
//        try {
//            boardService.updateBoard(boardId, boardUpdateDTO);
//        } catch (Throwable e) {
//            throw new Throwable(e.getMessage());
//        }
//
//        return new HttpResponse<>(
//                PrintResultMessage.BOARD_UPDATE_SUCCESS,
//                ResponseCode.ACCEPT,
//                null
//        );
//    }
//
//    @DeleteMapping(path = "/boards/remove")
//    public HttpResponse<?> removeBoard(Long boardId) throws Throwable {
//        try {
//            boardService.deleteBoard(boardId);
//        } catch (Throwable e) {
//            throw new Throwable(e.getMessage());
//        }
//
//        return new HttpResponse<>(
//                PrintResultMessage.BOARD_DELETE_SUCCESS,
//                ResponseCode.ACCEPT,
//                null
//        );
//
//    }
//
//    @GetMapping(path = "/boards/view")
//    public HttpResponse<?> viewBoard(String boardName) throws Exception {
//        List<Post> postList = postService.getPostsByBoardName(boardName);
//
//        List<Map<String, Object>> postSummaryList = postList.stream()
//                .map(post -> {
//                    Map<String, Object> postSummary = new HashMap<>();
//                    postSummary.put("게시글 번호", post.getPostId());
//                    postSummary.put("게시글 이름", post.getPostName());
//                    postSummary.put("작성일", post.getCreatedDate());
//                    return postSummary;
//                })
//                .toList();
//
//        return new HttpResponse<>(
//                ResponseCode.SUCCESS.getMessage(),
//                ResponseCode.SUCCESS,
//                postSummaryList
//        );
//
//    }
//
//}
