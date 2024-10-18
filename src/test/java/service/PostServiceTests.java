package service;

import com.sun.tools.javac.Main;
import config.Container;
import domain.Post;
import domain.grade.MemberGrade;
import dto.*;
import exception.UnauthorizedAccessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import repository.InMemoryBoardRepository;
import repository.InMemoryMemberRepository;
import repository.InMemoryPostRepository;
import util.RedisSessionManager;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.util.List;

public class PostServiceTests {

    private PostService postService;
    private MemberAuthService memberAuthService;
    private BoardService boardService;
    private MemberService memberService;

    @BeforeEach
    void setUp() throws Exception {
        Container container = Container.getInstance();
        container.scan("repository");
        container.scan("util");
        container.scan("service");
        container.scan("controller");

        InMemoryMemberRepository memberRepository = container.get(InMemoryMemberRepository.class);
        InMemoryBoardRepository boardRepository = container.get(InMemoryBoardRepository.class);
        InMemoryPostRepository postRepository = container.get(InMemoryPostRepository.class);
        RedisSessionManager redisSessionManager = container.get(RedisSessionManager.class);

        memberService = new MemberService(memberRepository);
        memberAuthService = new MemberAuthService(memberService, redisSessionManager);
        boardService = new BoardService(boardRepository, memberAuthService);
        postService = new PostService(postRepository, memberService, memberAuthService, boardService);
    }

    @Test
    void testCreatePostSuccessfullyWithAdmin() {
        memberService.registerAdminMember();
        MemberLoginDTO loginDTO = new MemberLoginDTO("admin", "admin");
        BoardRegisterDTO boardRegisterDTO = new BoardRegisterDTO(
                "Test Baord",
                "This is test-board",
                List.of(MemberGrade.ADMIN, MemberGrade.USER)
        );
        memberAuthService.login(loginDTO);
        boardService.createBoard(boardRegisterDTO);

        PostRegisterDTO postRegisterDTO = new PostRegisterDTO(
                "test post",
                "test post content",
                1L
        );

        postService.createPost(postRegisterDTO);
        assertEquals(1, postService.getAllPosts().size());
    }

    @Test
    void testCreatePostFailWithUser() {
        memberService.registerAdminMember();
        MemberLoginDTO loginDTO = new MemberLoginDTO("admin", "admin");
        BoardRegisterDTO boardRegisterDTO = new BoardRegisterDTO(
                "Test Baord",
                "This is test-board",
                List.of(MemberGrade.ADMIN)
        );
        memberAuthService.login(loginDTO);
        boardService.createBoard(boardRegisterDTO);
        memberAuthService.logout();

        MemberRegisterDTO memberDTO = new MemberRegisterDTO("yubin111", "yubin", "yubin@gmail.com", "qwer", LocalDate.now());
        memberService.registerMember(memberDTO);

        MemberLoginDTO memberLoginDTO = new MemberLoginDTO("yubin111", "qwer");
        memberAuthService.login(memberLoginDTO);

        PostRegisterDTO postRegisterDTO = new PostRegisterDTO(
                "test post",
                "test post content",
                1L
        );
        assertThrows(UnauthorizedAccessException.class, () -> postService.createPost(postRegisterDTO));
    }

    @Test
    void testCreatePostSuccessfullyWithMember() {
        memberService.registerAdminMember();
        MemberLoginDTO loginDTO = new MemberLoginDTO("admin", "admin");
        BoardRegisterDTO boardRegisterDTO = new BoardRegisterDTO(
                "Test Baord",
                "This is test-board",
                List.of(MemberGrade.ADMIN, MemberGrade.USER)
        );
        memberAuthService.login(loginDTO);
        boardService.createBoard(boardRegisterDTO);
        memberAuthService.logout();

        MemberRegisterDTO memberDTO = new MemberRegisterDTO("yubin111", "yubin", "yubin@gmail.com", "qwer", LocalDate.now());
        memberService.registerMember(memberDTO);

        MemberLoginDTO memberLoginDTO = new MemberLoginDTO("yubin111", "qwer");
        memberAuthService.login(memberLoginDTO);

        PostRegisterDTO postRegisterDTO = new PostRegisterDTO(
                "test post",
                "test post content",
                1L
        );

        postService.createPost(postRegisterDTO);
        assertEquals(1, postService.getAllPosts().size());
    }

    @Test
    void testUpdatePostSuccessfully() {
        memberService.registerAdminMember();
        MemberLoginDTO loginDTO = new MemberLoginDTO("admin", "admin");
        BoardRegisterDTO boardRegisterDTO = new BoardRegisterDTO(
                "Test Baord",
                "This is test-board",
                List.of(MemberGrade.ADMIN, MemberGrade.USER)
        );
        memberAuthService.login(loginDTO);
        boardService.createBoard(boardRegisterDTO);
        memberAuthService.logout();

        MemberRegisterDTO memberDTO = new MemberRegisterDTO("yubin111", "yubin", "yubin@gmail.com", "qwer", LocalDate.now());
        memberService.registerMember(memberDTO);

        MemberLoginDTO memberLoginDTO = new MemberLoginDTO("yubin111", "qwer");
        memberAuthService.login(memberLoginDTO);

        PostRegisterDTO postRegisterDTO = new PostRegisterDTO(
                "test post",
                "test post content",
                1L
        );
        postService.createPost(postRegisterDTO);

        Post post = postService.getAllPosts().getFirst();

        PostUpdateDTO postUpdateDTO = new PostUpdateDTO(
                post.getPostId(),
                post.getBoard().getBoardId(),
                "test post update",
                "test post content update"
        );
        postService.updatePost(postUpdateDTO);

        Post updatedPost = postService.getAllPosts().getFirst();
        assertEquals(updatedPost.getPostId(), post.getPostId());
        assertEquals(updatedPost.getBoard().getBoardId(), post.getBoard().getBoardId());
        assertEquals("test post update", updatedPost.getPostName());
        assertEquals("test post content update", updatedPost.getPostContent());
    }

    @Test
    void testUpdatePostSuccessfullyWithAdmin() {
        memberService.registerAdminMember();
        MemberLoginDTO loginDTO = new MemberLoginDTO("admin", "admin");
        BoardRegisterDTO boardRegisterDTO = new BoardRegisterDTO(
                "Test Baord",
                "This is test-board",
                List.of(MemberGrade.ADMIN, MemberGrade.USER)
        );
        memberAuthService.login(loginDTO);
        boardService.createBoard(boardRegisterDTO);
        memberAuthService.logout();

        MemberRegisterDTO memberDTO = new MemberRegisterDTO("yubin111", "yubin", "yubin@gmail.com", "qwer", LocalDate.now());
        memberService.registerMember(memberDTO);

        MemberLoginDTO memberLoginDTO = new MemberLoginDTO("yubin111", "qwer");
        memberAuthService.login(memberLoginDTO);

        PostRegisterDTO postRegisterDTO = new PostRegisterDTO(
                "test post",
                "test post content",
                1L
        );
        postService.createPost(postRegisterDTO);

        Post post = postService.getAllPosts().getFirst();

        PostUpdateDTO postUpdateDTO = new PostUpdateDTO(
                post.getPostId(),
                post.getBoard().getBoardId(),
                "test post update",
                "test post content update"
        );

        memberAuthService.logout();
        memberAuthService.login(loginDTO);
        postService.updatePost(postUpdateDTO);

        Post updatedPost = postService.getAllPosts().getFirst();
        assertEquals(updatedPost.getPostId(), post.getPostId());
        assertEquals(updatedPost.getBoard().getBoardId(), post.getBoard().getBoardId());
        assertEquals("test post update", updatedPost.getPostName());
        assertEquals("test post content update", updatedPost.getPostContent());
    }

    @Test
    void testUpdatePostFailWithUser() {
        memberService.registerAdminMember();
        MemberLoginDTO loginDTO = new MemberLoginDTO("admin", "admin");
        BoardRegisterDTO boardRegisterDTO = new BoardRegisterDTO(
                "Test Baord",
                "This is test-board",
                List.of(MemberGrade.ADMIN, MemberGrade.USER)
        );
        memberAuthService.login(loginDTO);
        boardService.createBoard(boardRegisterDTO);
        memberAuthService.logout();

        MemberRegisterDTO memberDTO = new MemberRegisterDTO("yubin111", "yubin", "yubin_other@gmail.com", "qwer", LocalDate.now());
        memberService.registerMember(memberDTO);

        MemberLoginDTO memberLoginDTO = new MemberLoginDTO("yubin111", "qwer");
        memberAuthService.login(memberLoginDTO);

        PostRegisterDTO postRegisterDTO = new PostRegisterDTO(
                "test post",
                "test post content",
                1L
        );
        postService.createPost(postRegisterDTO);

        Post post = postService.getAllPosts().getFirst();

        PostUpdateDTO postUpdateDTO = new PostUpdateDTO(
                post.getPostId(),
                post.getBoard().getBoardId(),
                "test post update",
                "test post content update"
        );

        memberAuthService.logout();

        MemberRegisterDTO otherMemberDTO = new MemberRegisterDTO("yu222", "yubin", "yubin@gmail.com", "qwer", LocalDate.now());
        memberService.registerMember(otherMemberDTO);

        MemberLoginDTO otherMemberLoginDTO = new MemberLoginDTO("yu222", "qwer");
        memberAuthService.login(otherMemberLoginDTO);
        assertThrows(UnauthorizedAccessException.class, () -> postService.updatePost(postUpdateDTO));
    }

    @Test
    void testDeletePostSuccessfullyWithAdmin() {
        memberService.registerAdminMember();
        MemberLoginDTO loginDTO = new MemberLoginDTO("admin", "admin");
        BoardRegisterDTO boardRegisterDTO = new BoardRegisterDTO(
                "Test Baord",
                "This is test-board",
                List.of(MemberGrade.ADMIN, MemberGrade.USER)
        );
        memberAuthService.login(loginDTO);
        boardService.createBoard(boardRegisterDTO);
        memberAuthService.logout();

        MemberRegisterDTO memberDTO = new MemberRegisterDTO("yubin111", "yubin", "yubin@gmail.com", "qwer", LocalDate.now());
        memberService.registerMember(memberDTO);

        MemberLoginDTO memberLoginDTO = new MemberLoginDTO("yubin111", "qwer");
        memberAuthService.login(memberLoginDTO);

        PostRegisterDTO postRegisterDTO = new PostRegisterDTO(
                "test post",
                "test post content",
                1L
        );
        postService.createPost(postRegisterDTO);

        memberAuthService.logout();
        memberAuthService.login(loginDTO);
        Post post = postService.getAllPosts().getFirst();
        postService.deletePost(post.getPostId());

        assertEquals(0, postService.getAllPosts().size());
    }

    @Test
    void testDeletePostSuccessfullyWithUser() {
        memberService.registerAdminMember();
        MemberLoginDTO loginDTO = new MemberLoginDTO("admin", "admin");
        BoardRegisterDTO boardRegisterDTO = new BoardRegisterDTO(
                "Test Baord",
                "This is test-board",
                List.of(MemberGrade.ADMIN, MemberGrade.USER)
        );
        memberAuthService.login(loginDTO);
        boardService.createBoard(boardRegisterDTO);
        memberAuthService.logout();

        MemberRegisterDTO memberDTO = new MemberRegisterDTO("yubin111", "yubin", "yubin@gmail.com", "qwer", LocalDate.now());
        memberService.registerMember(memberDTO);

        MemberLoginDTO memberLoginDTO = new MemberLoginDTO("yubin111", "qwer");
        memberAuthService.login(memberLoginDTO);

        PostRegisterDTO postRegisterDTO = new PostRegisterDTO(
                "test post",
                "test post content",
                1L
        );
        postService.createPost(postRegisterDTO);

        Post post = postService.getAllPosts().getFirst();
        postService.deletePost(post.getPostId());

        assertEquals(0, postService.getAllPosts().size());
    }

    @Test
    void testDeletePostFailWithUser() {
        memberService.registerAdminMember();
        MemberLoginDTO loginDTO = new MemberLoginDTO("admin", "admin");
        BoardRegisterDTO boardRegisterDTO = new BoardRegisterDTO(
                "Test Baord",
                "This is test-board",
                List.of(MemberGrade.ADMIN, MemberGrade.USER)
        );
        memberAuthService.login(loginDTO);
        boardService.createBoard(boardRegisterDTO);
        memberAuthService.logout();

        MemberRegisterDTO memberDTO = new MemberRegisterDTO("yubin111", "yubin", "yubin@gmail.com", "qwer", LocalDate.now());
        memberService.registerMember(memberDTO);

        MemberLoginDTO memberLoginDTO = new MemberLoginDTO("yubin111", "qwer");
        memberAuthService.login(memberLoginDTO);

        PostRegisterDTO postRegisterDTO = new PostRegisterDTO(
                "test post",
                "test post content",
                1L
        );
        postService.createPost(postRegisterDTO);
        memberAuthService.logout();

        MemberRegisterDTO otherMemberRegisterDTO = new MemberRegisterDTO("yubin121", "yubin", "yubi2n@gmail.com", "qwer", LocalDate.now());
        memberService.registerMember(otherMemberRegisterDTO);

        MemberLoginDTO otherMemberLoginDTO = new MemberLoginDTO("yubin121", "qwer");
        memberAuthService.login(otherMemberLoginDTO);

        Post post = postService.getAllPosts().getFirst();
        assertThrows(UnauthorizedAccessException.class, () -> postService.deletePost(post.getPostId()));
    }

}
