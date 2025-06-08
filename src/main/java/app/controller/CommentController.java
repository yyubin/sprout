package app.controller;

import sprout.beans.annotation.Controller;
import sprout.beans.annotation.Requires;
import sprout.mvc.annotation.DeleteMapping;
import sprout.mvc.annotation.GetMapping;
import sprout.mvc.annotation.PostMapping;
import sprout.mvc.annotation.PutMapping;
import app.domain.Comment;
import app.dto.CommentRegisterDTO;
import app.dto.CommentUpdateDTO;
import sprout.mvc.http.HttpResponse;
import sprout.mvc.http.ResponseCode;
import message.PrintResultMessage;
import app.service.interfaces.CommentServiceInterface;
import sprout.mvc.mapping.ControllerInterface;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@Requires(dependsOn = {CommentServiceInterface.class})
public class CommentController implements ControllerInterface {

    private final CommentServiceInterface commentService;

    public CommentController(CommentServiceInterface commentService) {
        this.commentService = commentService;
    }

    @GetMapping(path = "/comments/view")
    public HttpResponse<?> viewComment(Long boardId, Long postId) throws Throwable {
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

        return new HttpResponse<>(
                ResponseCode.SUCCESS.getMessage(),
                ResponseCode.SUCCESS,
                commentSummaryList
        );

    }

    @PostMapping(path = "/comments/add")
    public HttpResponse<?> createComment(CommentRegisterDTO commentRegisterDTO, Long boardId, Long postId) {
        commentService.createComment(commentRegisterDTO, boardId, postId);

        return new HttpResponse<>(
                PrintResultMessage.COMMENT_CREATE_SUCCESS,
                ResponseCode.CREATED,
                null
        );

    }

    @PutMapping(path = "/comments/update")
    public HttpResponse<?> updateComment(CommentUpdateDTO commentUpdateDTO, Long boardId, Long postId) throws Throwable {
        commentService.updateComment(commentUpdateDTO, boardId, postId);

        return new HttpResponse<>(
                PrintResultMessage.COMMENT_UPDATE_SUCCESS,
                ResponseCode.ACCEPT,
                null
        );

    }

    @DeleteMapping(path = "/comments/remove")
    public HttpResponse<?> deleteComment(Long boardId, Long postId, Long commentId) throws Throwable {
        commentService.deleteComment(commentId, boardId, postId);
        return new HttpResponse<>(
                PrintResultMessage.COMMENT_DELETE_SUCCESS,
                ResponseCode.ACCEPT,
                null
        );

    }
}
