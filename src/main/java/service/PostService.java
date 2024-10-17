package service;

import domain.Board;
import domain.Member;
import domain.Post;
import dto.PostRegisterDTO;
import dto.PostUpdateDTO;
import exception.MemberNotFoundException;
import exception.NotFoundBoardWithBoardIdException;
import exception.NotFoundPostWithPostIdException;
import exception.UnauthorizedAccessException;
import message.ExceptionMessage;
import repository.InMemoryPostRepository;
import util.Session;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public class PostService {

    private final InMemoryPostRepository postRepository;
    private final MemberService memberService;
    private final MemberAuthService memberAuthService;
    private final BoardService boardService;

    public PostService(InMemoryPostRepository postRepository, MemberService memberService, MemberAuthService memberAuthService, BoardService boardService) {
        this.postRepository = postRepository;
        this.memberService = memberService;
        this.memberAuthService = memberAuthService;
        this.boardService = boardService;
    }

    public void createPost(PostRegisterDTO postRegisterDTO) throws UnauthorizedAccessException, MemberNotFoundException, NotFoundBoardWithBoardIdException {
        String memberId = memberAuthService.getRedisSessionManager().getSession(Session.getSessionId());
        Board board = checkExistsBoardAndGetBoard(postRegisterDTO.getBoardId());
        Member author = checkExistsMemberAndGetMemberById(memberId);
        checkCreateAuthorityWithBoard(board, author);

        Post post = new Post(
                postRegisterDTO.getPostName(),
                postRegisterDTO.getPostContent(),
                author,
                board,
                LocalDateTime.now()
        );
        postRepository.save(post);
    }

    public void updatePost(PostUpdateDTO postUpdateDTO) throws UnauthorizedAccessException, NotFoundBoardWithBoardIdException {
        String memberId = memberAuthService.getRedisSessionManager().getSession(Session.getSessionId());
        Board board = checkExistsBoardAndGetBoard(postUpdateDTO.getBoardId());
        Member author = checkExistsMemberAndGetMemberById(memberId);
        checkCreateAuthorityWithBoard(board, author);
        checkPostOwnership(memberId, postUpdateDTO.getPostId());

        Post post = new Post(
                postUpdateDTO.getPostName(),
                postUpdateDTO.getPostContent(),
                author,
                board,
                LocalDateTime.now()
        );
        postRepository.update(post);
    }

    public void deletePost(Long postId) throws NotFoundPostWithPostIdException {
        String memberId = memberAuthService.getRedisSessionManager().getSession(Session.getSessionId());
        Board board = checkExistsBoardAndGetBoard(postId);
        Member author = checkExistsMemberAndGetMemberById(memberId);
        checkCreateAuthorityWithBoard(board, author);
        checkPostOwnership(memberId, postId);
        postRepository.deleteById(postId);
    }

    public List<Post> getPostsByBoardId(Long boardId) throws NotFoundBoardWithBoardIdException {
        return postRepository.findPostsByBoardId(boardId);
    }

    public List<Post> getAllPosts() {
        return postRepository.findAll();
    }

    public List<Post> getPostsByMemberId(String authorName) {
        return postRepository.findPostsByAuthor(authorName);
    }

    public List<Post> getPostsByPostName(String postName) {
        return postRepository.findPostsByName(postName);
    }

    private void checkCreateAuthorityWithBoard(Board board, Member author) throws UnauthorizedAccessException {
        if (!board.getAccessGrade().contains(author.getGrade())) {
            throw new UnauthorizedAccessException(ExceptionMessage.UNAUTHORIZED_CREATE_POST);
        }
    }

    private void checkPostOwnership(String memberId, Long postId) throws UnauthorizedAccessException {
        Post post = postRepository.findById(postId).orElseThrow(NotFoundPostWithPostIdException::new);
        if (!post.getAuthor().getId().equals(memberId)) {
            throw new UnauthorizedAccessException(ExceptionMessage.UNAUTHORIZED_POST);
        }
    }

    private Board checkExistsBoardAndGetBoard(Long boardId) throws NotFoundBoardWithBoardIdException {
        return boardService.getBoardById(boardId).orElseThrow(NotFoundBoardWithBoardIdException::new);
    }

    private Member checkExistsMemberAndGetMemberById(String memberId) throws MemberNotFoundException {
        return memberService.getMemberById(memberId).orElseThrow(MemberNotFoundException::new);
    }


}
