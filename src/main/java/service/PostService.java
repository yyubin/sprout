package service;

import config.Container;
import config.annotations.Priority;
import config.annotations.Requires;
import config.annotations.Service;
import domain.Board;
import domain.Member;
import domain.Post;
import domain.grade.MemberGrade;
import dto.PostRegisterDTO;
import dto.PostUpdateDTO;
import exception.MemberNotFoundException;
import exception.NotFoundBoardWithBoardIdException;
import exception.NotFoundPostWithPostIdException;
import exception.UnauthorizedAccessException;
import message.ExceptionMessage;
import repository.InMemoryPostRepository;
import repository.interfaces.PostRepository;
import service.interfaces.PostServiceInterface;
import util.Session;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Priority(value = 3)
@Requires(dependsOn = {PostRepository.class, MemberService.class, MemberAuthService.class, BoardService.class})
public class PostService implements PostServiceInterface {

    private final PostRepository postRepository;
    private final MemberService memberService;
    private final MemberAuthService memberAuthService;
    private final BoardService boardService;

    public PostService(PostRepository postRepository, MemberService memberService, MemberAuthService memberAuthService, BoardService boardService) {
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

    public void updatePost(PostUpdateDTO postUpdateDTO) throws UnauthorizedAccessException, NotFoundBoardWithBoardIdException, NotFoundPostWithPostIdException {
        String memberId = memberAuthService.getRedisSessionManager().getSession(Session.getSessionId());
        Board board = checkExistsBoardAndGetBoard(postUpdateDTO.getBoardId());
        Member author = checkExistsMemberAndGetMemberById(memberId);
        checkCreateAuthorityWithBoard(board, author);
        checkPostOwnership(memberId, postUpdateDTO.getPostId());

        Post post = postRepository.findById(postUpdateDTO.getPostId()).orElseThrow(() -> new NotFoundPostWithPostIdException(ExceptionMessage.NOT_FOUND_POST_WITH_POST_ID, postUpdateDTO.getPostId()));

        post.setPostName(postUpdateDTO.getPostName());
        post.setPostContent(postUpdateDTO.getPostContent());
        post.setUpdatedDate(LocalDateTime.now());
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

    public List<Post> getPostsByBoardName(String boardName) {
        return getAllPosts().stream()
                .filter(post -> post.getBoard().getBoardName().equals(boardName) && !post.isDeleted())
                .toList();
    }

    public Post getPost(Long postId, Long boardId) throws NotFoundPostWithPostIdException {
        return postRepository.findByPostIdAndBoardId(postId, boardId)
                .orElseThrow(() -> new NotFoundPostWithPostIdException(ExceptionMessage.NOT_FOUND_POST_WITH_POST_ID, postId));
    }

    public int getAllPostsSize() {
        return postRepository.allPostsSize();
    }

    public int getPostsSizeWithBoardId(Long boardId) {
        return postRepository.postsSizeWithBoard(boardId);
    }

    private void checkCreateAuthorityWithBoard(Board board, Member author) throws UnauthorizedAccessException {
        if (!board.getAccessGrade().contains(author.getGrade())) {
            throw new UnauthorizedAccessException(ExceptionMessage.UNAUTHORIZED_CREATE_POST);
        }
    }

    private void checkPostOwnership(String memberId, Long postId) throws UnauthorizedAccessException {
        Member member = checkExistsMemberAndGetMemberById(memberId);
        if (member.getGrade() == MemberGrade.ADMIN) {
            return; // ADMIN은 모든 포스트에 대한 접근 가능
        }

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new NotFoundPostWithPostIdException(ExceptionMessage.NOT_FOUND_POST_WITH_POST_ID, postId));
        if (!post.getAuthor().getId().equals(memberId)) {
            throw new UnauthorizedAccessException(ExceptionMessage.UNAUTHORIZED_POST);
        }
    }

    private Board checkExistsBoardAndGetBoard(Long boardId) throws NotFoundBoardWithBoardIdException {
        return boardService.getBoardById(boardId)
                .orElseThrow(() -> new NotFoundBoardWithBoardIdException(ExceptionMessage.NOT_FOUND_BOARD_WITH_BOARD_ID, boardId));
    }

    private Member checkExistsMemberAndGetMemberById(String memberId) throws MemberNotFoundException {
        return memberService.getMemberById(memberId)
                .orElseThrow(() -> new MemberNotFoundException(ExceptionMessage.MEMBER_NOT_FOUND));
    }


}
