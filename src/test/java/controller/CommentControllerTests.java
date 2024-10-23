package controller;

import domain.Comment;
import dto.CommentRegisterDTO;
import dto.CommentUpdateDTO;
import http.response.HttpResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import service.interfaces.CommentServiceInterface;
import view.interfaces.PrintProcessor;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class CommentControllerTests {

    @Mock
    private CommentServiceInterface commentService;

    @Mock
    private PrintProcessor printHandler;

    @InjectMocks
    private CommentController commentController;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testCreateComment() {
        Long boardId = 1L; // Sample board ID
        Long postId = 1L;  // Sample post ID
        CommentRegisterDTO commentRegisterDTO = new CommentRegisterDTO(null, "This is a test comment.");

        commentController.createComment(commentRegisterDTO, boardId, postId);

        verify(commentService, times(1)).createComment(eq(commentRegisterDTO), eq(boardId), eq(postId));
        verify(printHandler, times(1)).printSuccessWithResponseCodeAndCustomMessage(any(HttpResponse.class));
    }

    @Test
    public void testUpdateComment() throws Throwable {
        Long boardId = 1L;  // Sample board ID
        Long postId = 1L;   // Sample post ID
        CommentUpdateDTO commentUpdateDTO = new CommentUpdateDTO(1L, "Updated comment content");

        commentController.updateComment(commentUpdateDTO, boardId, postId);

        verify(commentService, times(1)).updateComment(eq(commentUpdateDTO), eq(boardId), eq(postId));
        verify(printHandler, times(1)).printSuccessWithResponseCodeAndCustomMessage(any(HttpResponse.class));
    }

    @Test
    public void testDeleteComment() throws Throwable {
        Long boardId = 1L;  // Sample board ID
        Long postId = 1L;   // Sample post ID
        Long commentId = 1L;  // Sample comment ID

        commentController.deleteComment(boardId, postId, commentId);

        verify(commentService, times(1)).deleteComment(eq(commentId), eq(boardId), eq(postId));
        verify(printHandler, times(1)).printSuccessWithResponseCodeAndCustomMessage(any(HttpResponse.class));
    }

    @Test
    void viewComment_shouldReturnCommentSummary() throws Throwable {
        Long boardId = 1L;
        Long postId = 1L;

        Comment parentComment = new Comment(1L, 1L, 1L, null, "Author1", "Parent", LocalDateTime.now());
        Comment childComment = new Comment(2L, 1L, 1L, 1L, "Author2", "Child", LocalDateTime.now());

        List<Comment> comments = Arrays.asList(parentComment, childComment);

        when(commentService.viewComments(boardId, postId)).thenReturn(comments);

        commentController.viewComment(boardId, postId);

        Map<String, Object> parentCommentSummary = new HashMap<>();
        parentCommentSummary.put("댓글 내용", "Parent comment");
        parentCommentSummary.put("작성자", "Author1");

        Map<String, Object> childCommentSummary = new HashMap<>();
        childCommentSummary.put("댓글 내용", "- Child comment");
        childCommentSummary.put("작성자", "Author2");

        List<Map<String, Object>> expectedSummaryList = Arrays.asList(parentCommentSummary, childCommentSummary);

        verify(printHandler).printResponseBodyAsMapList(any());
    }

}
