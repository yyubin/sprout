package service;

import com.sun.tools.javac.Main;
import config.Container;
import config.PackageName;
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
        Container.getInstance().scan(PackageName.repository.getPackageName());
        Container.getInstance().scan(PackageName.util.getPackageName());
        Container.getInstance().scan(PackageName.service.getPackageName());

        memberService = Container.getInstance().get(MemberService.class);
        memberAuthService = Container.getInstance().get(MemberAuthService.class);
        boardService = Container.getInstance().get(BoardService.class);
        postService = Container.getInstance().get(PostService.class);
    }

    @Test
    void testCreatePostSuccessfullyWithAdmin() throws Throwable {
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
    void testCreatePostFailWithUser() throws Throwable {
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
    void testCreatePostSuccessfullyWithMember() throws Throwable {
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
    void testUpdatePostSuccessfully() throws Throwable {
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
    void testUpdatePostSuccessfullyWithAdmin() throws Throwable {
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
    void testUpdatePostFailWithUser() throws Throwable {
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
    void testDeletePostSuccessfullyWithAdmin() throws Throwable {
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
    void testDeletePostSuccessfullyWithUser() throws Throwable {
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
    void testDeletePostFailWithUser() throws Throwable {
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
