package app.service;

import legacy.config.annotations.Priority;
import sprout.beans.annotation.Requires;
import sprout.beans.annotation.Service;
import app.domain.Comment;
import app.domain.grade.MemberGrade;
import app.dto.CommentRegisterDTO;
import app.dto.CommentUpdateDTO;
import app.exception.NotFoundComment;
import app.exception.NotLoggedInException;
import app.exception.UnauthorizedAccessException;
import app.message.ExceptionMessage;
import app.repository.interfaces.CommentRepository;
import app.service.interfaces.CommentServiceInterface;
import app.service.interfaces.MemberAuthServiceInterface;
import app.util.Session;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Priority(value = 4)
@Requires(dependsOn = {CommentRepository.class, MemberAuthServiceInterface.class})
public class CommentService implements CommentServiceInterface {

    private final CommentRepository commentRepository;
    private final MemberAuthServiceInterface memberAuthService;

    public CommentService(CommentRepository commentRepository, MemberAuthServiceInterface memberAuthService) {
        this.commentRepository = commentRepository;
        this.memberAuthService = memberAuthService;
    }

    private void checkLogin() throws UnauthorizedAccessException {
        if (Session.getSessionId() == null) {
            throw new NotLoggedInException(ExceptionMessage.NOT_LOGGED_IN);
        }
    }

    public void createComment(CommentRegisterDTO commentRegisterDTO, Long boardId, Long postId) {
        checkLogin();
        Comment comment = new Comment(
                (long) commentRepository.getNewCommentId(boardId, postId),
                boardId,
                postId,
                commentRegisterDTO.getParentCommentId(),
                memberAuthService.getRedisSessionManager().getSession(Session.getSessionId()),
                commentRegisterDTO.getContent(),
                LocalDateTime.now()
        );
        commentRepository.save(comment);
    }

    public void deleteComment(Long commentId, Long boardId, Long postId) throws Throwable {
        checkLogin();
        Optional<Comment> validComment = getValidComment(commentId, boardId, postId);
        validComment.ifPresent(comment -> commentRepository.delete(comment.getId(), boardId, postId));
    }

    public void updateComment(CommentUpdateDTO commentUpdateDTO, Long boardId, Long postId) throws Throwable {
        checkLogin();
        Optional<Comment> validComment = getValidComment(commentUpdateDTO.getCommentId(), boardId, postId);
        if (validComment.isPresent()) {
            Comment comment = validComment.get();
            comment.setUpdatedAt(LocalDateTime.now());
            comment.setContent(commentUpdateDTO.getContent());
            commentRepository.update(comment);
        }
    }

    public List<Comment> viewComments(Long boardId, Long postId) throws Throwable {
        return commentRepository.findByBoardIdAndPostId(boardId, postId);
    }

    public Optional<Comment> getValidComment(Long commentId, Long boardId, Long postId) throws Throwable {
        Optional<Comment> comment = commentRepository.findById(commentId, boardId, postId);
        if (comment.isEmpty()) {
            throw new NotFoundComment(ExceptionMessage.NOT_FOUND_COMMENT);
        }
        if (MemberGrade.ADMIN == memberAuthService.checkAuthority(Session.getSessionId())) {
            return comment;
        }
        if (!memberAuthService.getRedisSessionManager().getSession(Session.getSessionId()).equals(comment.get().getAuthor())) {
            throw new UnauthorizedAccessException(ExceptionMessage.UNAUTHORIZED_UPDATE_COMMENT);
        }
        return comment;
    }

}
