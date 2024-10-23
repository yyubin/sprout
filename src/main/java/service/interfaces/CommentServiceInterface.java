package service.interfaces;

import domain.Comment;
import dto.CommentRegisterDTO;
import dto.CommentUpdateDTO;

import java.util.List;
import java.util.Optional;

public interface CommentServiceInterface {

    void createComment(CommentRegisterDTO commentRegisterDTO, Long boardId, Long postId);

    void deleteComment(Long commentId, Long boardId, Long postId) throws Throwable;

    void updateComment(CommentUpdateDTO commentUpdateDTO, Long boardId, Long postId) throws Throwable;

    List<Comment> viewComments(Long boardId, Long postId) throws Throwable;

    Optional<Comment> getValidComment(Long commentId, Long boardId, Long postId) throws Throwable;

}
