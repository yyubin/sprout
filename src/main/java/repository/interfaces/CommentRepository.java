package repository.interfaces;

import domain.Comment;

import java.util.List;
import java.util.Optional;

public interface CommentRepository {
    void save(Comment comment);
    void update(Comment comment);
    void delete(Long id, Long boardId, Long postId);
    List<Comment> findAll();
    List<Comment> findByBoardIdAndPostId(Long boardId, Long postId);
    Optional<Comment> findById(Long id, Long boardId, Long postId);
    int size();
    int getNewCommentId(Long boardId, Long postId);
}
