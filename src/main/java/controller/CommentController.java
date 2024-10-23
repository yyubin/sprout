package controller;

import config.annotations.Controller;
import config.annotations.Requires;
import controller.annotations.DeleteMapping;
import controller.annotations.GetMapping;
import controller.annotations.PostMapping;
import controller.annotations.PutMapping;
import domain.Comment;
import dto.CommentRegisterDTO;
import dto.CommentUpdateDTO;
import http.response.HttpResponse;
import http.response.ResponseCode;
import message.PrintResultMessage;
import service.interfaces.CommentServiceInterface;
import view.interfaces.PrintProcessor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@Requires(dependsOn = {CommentServiceInterface.class, PrintProcessor.class})
public class CommentController implements ControllerInterface{

    private final CommentServiceInterface commentService;
    private final PrintProcessor printHandler;

    public CommentController(PrintProcessor printHandler, CommentServiceInterface commentService) {
        this.printHandler = printHandler;
        this.commentService = commentService;
    }

    @GetMapping(path = "/comments/view")
    public void viewComment(Long boardId, Long postId) throws Throwable {
        List<Comment> comments = commentService.viewComments(boardId, postId);

        List<Map<String, Object>> commentSummaryList = comments.stream()
                .map(comment -> {
                    Map<String, Object> commentSummary = new HashMap<>();
                    commentSummary.put("댓글 내용", comment.getContent());
                    commentSummary.put("작성자", comment.getAuthor());
                    if (comment.getParentCommentId() != null) {
                        commentSummary.put("댓글 내용", "- " + comment.getContent());
                    }
                    return commentSummary;
                })
                .toList();

        HttpResponse<List<Map<String, Object>>> response = new HttpResponse<>(
                ResponseCode.SUCCESS.getMessage(),
                ResponseCode.SUCCESS,
                commentSummaryList
        );

        printHandler.printResponseBodyAsMapList(response);
    }

    @PostMapping(path = "/comments/add")
    public void createComment(CommentRegisterDTO commentRegisterDTO, Long boardId, Long postId) {
        commentService.createComment(commentRegisterDTO, boardId, postId);

        HttpResponse<?> response = new HttpResponse<>(
                PrintResultMessage.COMMENT_CREATE_SUCCESS,
                ResponseCode.SUCCESS,
                null
        );
        printHandler.printSuccessWithResponseCodeAndCustomMessage(response);
    }

    @PutMapping(path = "/comments/update")
    public void updateComment(CommentUpdateDTO commentUpdateDTO, Long boardId, Long postId) throws Throwable {
        commentService.updateComment(commentUpdateDTO, boardId, postId);

        HttpResponse<?> response = new HttpResponse<>(
                PrintResultMessage.COMMENT_UPDATE_SUCCESS,
                ResponseCode.SUCCESS,
                null
        );
        printHandler.printSuccessWithResponseCodeAndCustomMessage(response);
    }

    @DeleteMapping(path = "/comments/remove")
    public void deleteComment(Long boardId, Long postId, Long commentId) throws Throwable {
        commentService.deleteComment(commentId, boardId, postId);
        HttpResponse<?> response = new HttpResponse<>(
                PrintResultMessage.COMMENT_DELETE_SUCCESS,
                ResponseCode.SUCCESS,
                null
        );
        printHandler.printSuccessWithResponseCodeAndCustomMessage(response);
    }
}
