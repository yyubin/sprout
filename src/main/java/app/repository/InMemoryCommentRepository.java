package app.repository;

import sprout.beans.annotation.Repository;
import app.domain.Comment;
import app.repository.interfaces.CommentRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class InMemoryCommentRepository implements CommentRepository {
    private final List<Comment> comments = new ArrayList<>();

    @Override
    public void save(Comment comment) {
        comments.add(comment);
    }

    @Override
    public void update(Comment comment) {
        comments.stream()
                .filter(c -> c.getId().equals(comment.getId()) &&
                        c.getBoardId().equals(comment.getBoardId()) &&
                        c.getPostId().equals(comment.getPostId()) &&
                        !c.isDeleted())
                .findFirst()
                .ifPresent(existingComment -> {
                    existingComment.setContent(comment.getContent());
                    existingComment.setUpdatedAt(comment.getUpdatedAt());
                });
    }

    @Override
    public void delete(Long id, Long boardId, Long postId) {
        comments.stream()
                .filter(c -> c.getId().equals(id) &&
                        c.getBoardId().equals(boardId) &&
                        c.getPostId().equals(postId) &&
                        !c.isDeleted())
                .findFirst()
                .ifPresent(comment -> comment.setDeleted(true));
    }

    @Override
    public List<Comment> findAll() {
        return comments.stream()
                .filter(comment -> !comment.isDeleted())
                .collect(Collectors.toList());
    }

    @Override
    public List<Comment> findByBoardIdAndPostId(Long boardId, Long postId) {
        return comments.stream()
                .filter(comment -> comment.getBoardId().equals(boardId) &&
                        comment.getPostId().equals(postId) &&
                        !comment.isDeleted())
                .collect(Collectors.toList());
    }

    @Override
    public Optional<Comment> findById(Long id, Long boardId, Long postId) {
        return comments.stream()
                .filter(comment -> comment.getId().equals(id) &&
                        comment.getBoardId().equals(boardId) &&
                        comment.getPostId().equals(postId) &&
                        !comment.isDeleted())
                .findFirst();
    }

    @Override
    public int size() {
        return (int) comments.stream()
                .filter(comment -> !comment.isDeleted())
                .count();
    }

    @Override
    public int getNewCommentId(Long boardId, Long postId) {
        return (int) comments.stream()
                .filter(comment -> comment.getBoardId().equals(boardId) &&
                        comment.getPostId().equals(postId))
                .count() + 1;
    }
}
