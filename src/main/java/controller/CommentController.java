package controller;

import config.annotations.Controller;
import config.annotations.Requires;
import controller.annotations.DeleteMapping;
import controller.annotations.GetMapping;
import controller.annotations.PostMapping;
import controller.annotations.PutMapping;
import dto.CommentRegisterDTO;
import dto.CommentUpdateDTO;
import http.response.HttpResponse;
import http.response.ResponseCode;
import message.PrintResultMessage;
import service.interfaces.CommentServiceInterface;
import view.interfaces.PrintProcessor;

@Controller
@Requires(dependsOn = {CommentServiceInterface.class, PrintProcessor.class})
public class CommentController implements ControllerInterface{

    private final CommentServiceInterface commentService;
    private final PrintProcessor printHandler;

    public CommentController(PrintProcessor printHandler, CommentServiceInterface commentService) {
        this.printHandler = printHandler;
        this.commentService = commentService;
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
