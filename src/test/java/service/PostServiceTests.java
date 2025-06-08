package service;

import app.dto.*;
import config.Container;
import config.PackageName;
import app.domain.Post;
import exception.UnauthorizedAccessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import app.service.interfaces.BoardServiceInterface;
import app.service.interfaces.MemberAuthServiceInterface;
import app.service.interfaces.MemberServiceInterface;
import app.service.interfaces.PostServiceInterface;

import static org.junit.jupiter.api.Assertions.*;

public class PostServiceTests {

    private PostServiceInterface postService;
    private MemberAuthServiceInterface memberAuthService;
    private BoardServiceInterface boardService;
    private MemberServiceInterface memberService;

    @BeforeEach
    void setUp() throws Exception {
        Container.getInstance().scan(PackageName.repository.getPackageName());
        Container.getInstance().scan(PackageName.util.getPackageName());
        Container.getInstance().scan(PackageName.service.getPackageName());

        memberService = Container.getInstance().get(MemberServiceInterface.class);
        memberAuthService = Container.getInstance().get(MemberAuthServiceInterface.class);
        boardService = Container.getInstance().get(BoardServiceInterface.class);
        postService = Container.getInstance().get(PostServiceInterface.class);
    }

    @Test
    void testCreatePostSuccessfullyWithAdmin() throws Throwable {
        memberService.registerAdminMember();
        MemberLoginDTO loginDTO = new MemberLoginDTO("admin", "admin");
        BoardRegisterDTO boardRegisterDTO = new BoardRegisterDTO(
                "Test Baord",
                "This is test-board",
                "USER"
        );
        memberAuthService.login(loginDTO);
        boardService.createBoard(boardRegisterDTO);

        PostRegisterDTO postRegisterDTO = new PostRegisterDTO(
                "test post",
                "test post content"
        );

        postService.createPost(1L, postRegisterDTO);
        assertEquals(1, postService.getAllPosts().size());
    }

    @Test
    void testCreatePostFailWithUser() throws Throwable {
        memberService.registerAdminMember();
        MemberLoginDTO loginDTO = new MemberLoginDTO("admin", "admin");
        BoardRegisterDTO boardRegisterDTO = new BoardRegisterDTO(
                "Test Baord",
                "This is test-board",
                "ADMIN"
        );
        memberAuthService.login(loginDTO);
        boardService.createBoard(boardRegisterDTO);
        memberAuthService.logout();

        MemberRegisterDTO memberDTO = new MemberRegisterDTO("yubin111", "yubin", "yubin@gmail.com", "qwer");
        memberService.registerMember(memberDTO);

        MemberLoginDTO memberLoginDTO = new MemberLoginDTO("yubin111", "qwer");
        memberAuthService.login(memberLoginDTO);

        PostRegisterDTO postRegisterDTO = new PostRegisterDTO(
                "test post",
                "test post content"
        );
        assertThrows(UnauthorizedAccessException.class, () -> postService.createPost(1L, postRegisterDTO));
    }

    @Test
    void testCreatePostSuccessfullyWithMember() throws Throwable {
        memberService.registerAdminMember();
        MemberLoginDTO loginDTO = new MemberLoginDTO("admin", "admin");
        BoardRegisterDTO boardRegisterDTO = new BoardRegisterDTO(
                "Test Baord",
                "This is test-board",
                "USER"
        );
        memberAuthService.login(loginDTO);
        boardService.createBoard(boardRegisterDTO);
        memberAuthService.logout();

        MemberRegisterDTO memberDTO = new MemberRegisterDTO("yubin111", "yubin", "yubin@gmail.com", "qwer");
        memberService.registerMember(memberDTO);

        MemberLoginDTO memberLoginDTO = new MemberLoginDTO("yubin111", "qwer");
        memberAuthService.login(memberLoginDTO);

        PostRegisterDTO postRegisterDTO = new PostRegisterDTO(
                "test post",
                "test post content"
        );

        postService.createPost(1L, postRegisterDTO);
        assertEquals(1, postService.getAllPosts().size());
    }

    @Test
    void testUpdatePostSuccessfully() throws Throwable {
        memberService.registerAdminMember();
        MemberLoginDTO loginDTO = new MemberLoginDTO("admin", "admin");
        BoardRegisterDTO boardRegisterDTO = new BoardRegisterDTO(
                "Test Baord",
                "This is test-board",
                "USER"
        );
        memberAuthService.login(loginDTO);
        boardService.createBoard(boardRegisterDTO);
        memberAuthService.logout();

        MemberRegisterDTO memberDTO = new MemberRegisterDTO("yubin111", "yubin", "yubin@gmail.com", "qwer");
        memberService.registerMember(memberDTO);

        MemberLoginDTO memberLoginDTO = new MemberLoginDTO("yubin111", "qwer");
        memberAuthService.login(memberLoginDTO);

        PostRegisterDTO postRegisterDTO = new PostRegisterDTO(
                "test post",
                "test post content"
        );
        postService.createPost(1L, postRegisterDTO);

        Post post = postService.getAllPosts().getFirst();

        PostUpdateDTO postUpdateDTO = new PostUpdateDTO(
                "test post update",
                "test post content update"
        );
        postService.updatePost(1L, 1L, postUpdateDTO);

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
                "USER"
        );
        memberAuthService.login(loginDTO);
        boardService.createBoard(boardRegisterDTO);
        memberAuthService.logout();

        MemberRegisterDTO memberDTO = new MemberRegisterDTO("yubin111", "yubin", "yubin@gmail.com", "qwer");
        memberService.registerMember(memberDTO);

        MemberLoginDTO memberLoginDTO = new MemberLoginDTO("yubin111", "qwer");
        memberAuthService.login(memberLoginDTO);

        PostRegisterDTO postRegisterDTO = new PostRegisterDTO(
                "test post",
                "test post content"
        );
        postService.createPost(1L, postRegisterDTO);

        Post post = postService.getAllPosts().getFirst();

        PostUpdateDTO postUpdateDTO = new PostUpdateDTO(
                "test post update",
                "test post content update"
        );

        memberAuthService.logout();
        memberAuthService.login(loginDTO);
        postService.updatePost(1L,1L, postUpdateDTO);

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
                "USER"
        );
        memberAuthService.login(loginDTO);
        boardService.createBoard(boardRegisterDTO);
        memberAuthService.logout();

        MemberRegisterDTO memberDTO = new MemberRegisterDTO("yubin111", "yubin", "yubin_other@gmail.com", "qwer");
        memberService.registerMember(memberDTO);

        MemberLoginDTO memberLoginDTO = new MemberLoginDTO("yubin111", "qwer");
        memberAuthService.login(memberLoginDTO);

        PostRegisterDTO postRegisterDTO = new PostRegisterDTO(
                "test post",
                "test post content"
        );
        postService.createPost(1L, postRegisterDTO);

        Post post = postService.getAllPosts().getFirst();

        PostUpdateDTO postUpdateDTO = new PostUpdateDTO(
                "test post update",
                "test post content update"
        );

        memberAuthService.logout();

        MemberRegisterDTO otherMemberDTO = new MemberRegisterDTO("yu222", "yubin", "yubin@gmail.com", "qwer");
        memberService.registerMember(otherMemberDTO);

        MemberLoginDTO otherMemberLoginDTO = new MemberLoginDTO("yu222", "qwer");
        memberAuthService.login(otherMemberLoginDTO);
        assertThrows(UnauthorizedAccessException.class, () -> postService.updatePost(1L, 1L, postUpdateDTO));
    }

    @Test
    void testDeletePostSuccessfullyWithAdmin() throws Throwable {
        memberService.registerAdminMember();
        MemberLoginDTO loginDTO = new MemberLoginDTO("admin", "admin");
        BoardRegisterDTO boardRegisterDTO = new BoardRegisterDTO(
                "Test Baord",
                "This is test-board",
                "USER"
        );
        memberAuthService.login(loginDTO);
        boardService.createBoard(boardRegisterDTO);
        memberAuthService.logout();

        MemberRegisterDTO memberDTO = new MemberRegisterDTO("yubin111", "yubin", "yubin@gmail.com", "qwer");
        memberService.registerMember(memberDTO);

        MemberLoginDTO memberLoginDTO = new MemberLoginDTO("yubin111", "qwer");
        memberAuthService.login(memberLoginDTO);

        PostRegisterDTO postRegisterDTO = new PostRegisterDTO(
                "test post",
                "test post content"
        );
        postService.createPost(1L, postRegisterDTO);

        memberAuthService.logout();
        memberAuthService.login(loginDTO);
        Post post = postService.getAllPosts().getFirst();
        postService.deletePost(1L, post.getPostId());

        assertEquals(0, postService.getAllPosts().size());
    }

    @Test
    void testDeletePostSuccessfullyWithUser() throws Throwable {
        memberService.registerAdminMember();
        MemberLoginDTO loginDTO = new MemberLoginDTO("admin", "admin");
        BoardRegisterDTO boardRegisterDTO = new BoardRegisterDTO(
                "Test Baord",
                "This is test-board",
                "USER"
        );
        memberAuthService.login(loginDTO);
        boardService.createBoard(boardRegisterDTO);
        memberAuthService.logout();

        MemberRegisterDTO memberDTO = new MemberRegisterDTO("yubin111", "yubin", "yubin@gmail.com", "qwer");
        memberService.registerMember(memberDTO);

        MemberLoginDTO memberLoginDTO = new MemberLoginDTO("yubin111", "qwer");
        memberAuthService.login(memberLoginDTO);

        PostRegisterDTO postRegisterDTO = new PostRegisterDTO(
                "test post",
                "test post content"
        );
        postService.createPost(1L, postRegisterDTO);

        Post post = postService.getAllPosts().getFirst();
        postService.deletePost(1L, post.getPostId());

        assertEquals(0, postService.getAllPosts().size());
    }

    @Test
    void testDeletePostFailWithUser() throws Throwable {
        memberService.registerAdminMember();
        MemberLoginDTO loginDTO = new MemberLoginDTO("admin", "admin");
        BoardRegisterDTO boardRegisterDTO = new BoardRegisterDTO(
                "Test Baord",
                "This is test-board",
                "USER"
        );
        memberAuthService.login(loginDTO);
        boardService.createBoard(boardRegisterDTO);
        memberAuthService.logout();

        MemberRegisterDTO memberDTO = new MemberRegisterDTO("yubin111", "yubin", "yubin@gmail.com", "qwer");
        memberService.registerMember(memberDTO);

        MemberLoginDTO memberLoginDTO = new MemberLoginDTO("yubin111", "qwer");
        memberAuthService.login(memberLoginDTO);

        PostRegisterDTO postRegisterDTO = new PostRegisterDTO(
                "test post",
                "test post content"
        );
        postService.createPost(1L, postRegisterDTO);
        memberAuthService.logout();

        MemberRegisterDTO otherMemberRegisterDTO = new MemberRegisterDTO("yubin121", "yubin", "yubi2n@gmail.com", "qwer");
        memberService.registerMember(otherMemberRegisterDTO);

        MemberLoginDTO otherMemberLoginDTO = new MemberLoginDTO("yubin121", "qwer");
        memberAuthService.login(otherMemberLoginDTO);

        Post post = postService.getAllPosts().getFirst();
        assertThrows(UnauthorizedAccessException.class, () -> postService.deletePost(1L, post.getPostId()));
    }

}
