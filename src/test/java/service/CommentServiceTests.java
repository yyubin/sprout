package service;

import config.Container;
import config.PackageName;
import dto.*;
import exception.NotLoggedInException;
import exception.UnauthorizedAccessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import service.interfaces.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class CommentServiceTests {

    private MemberServiceInterface memberService;
    private MemberAuthServiceInterface memberAuthService;
    private BoardServiceInterface boardService;
    private PostServiceInterface postService;
    private CommentServiceInterface commentService;

    @BeforeEach
    void setUp() throws Throwable {
        Container.getInstance().scan(PackageName.repository.getPackageName());
        Container.getInstance().scan(PackageName.util.getPackageName());
        Container.getInstance().scan(PackageName.service.getPackageName());

        memberService = Container.getInstance().get(MemberServiceInterface.class);
        memberAuthService = Container.getInstance().get(MemberAuthServiceInterface.class);
        boardService = Container.getInstance().get(BoardServiceInterface.class);
        postService = Container.getInstance().get(PostServiceInterface.class);
        commentService = Container.getInstance().get(CommentServiceInterface.class);

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

        MemberRegisterDTO memberDTO = new MemberRegisterDTO("yubin", "yubin", "yubin@gmail.com", "qwer");
        memberService.registerMember(memberDTO);
    }

    @Test
    void testAddCommentSuccessfully() throws Throwable {
        MemberLoginDTO loginDTO = new MemberLoginDTO("admin", "admin");
        memberAuthService.login(loginDTO);

        PostRegisterDTO postRegisterDTO = new PostRegisterDTO(
                "Test Post",
                "This is a test post."
        );
        postService.createPost(1L, postRegisterDTO);

        CommentRegisterDTO commentRegisterDTO = new CommentRegisterDTO(
                null,
                "Test comment"
        );
        commentService.createComment(commentRegisterDTO, 1L, 1L);

        assertEquals(1, commentService.viewComments(1L, 1L).size());
    }

    @Test
    void testAddCommentFailWithoutLogin() throws Throwable {
        MemberLoginDTO loginDTO = new MemberLoginDTO("admin", "admin");
        memberAuthService.login(loginDTO);
        PostRegisterDTO postRegisterDTO = new PostRegisterDTO(
                "Test Post",
                "This is a test post."
        );
        postService.createPost(1L, postRegisterDTO);

        memberAuthService.logout();

        CommentRegisterDTO commentRegisterDTO = new CommentRegisterDTO(
                null,
                "Test comment"
        );

        assertThrows(NotLoggedInException.class, () -> commentService.createComment(commentRegisterDTO, 1L, 1L));
    }

    @Test
    void testUpdateCommentSuccessfully() throws Throwable {
        MemberLoginDTO loginDTO = new MemberLoginDTO("admin", "admin");
        memberAuthService.login(loginDTO);

        PostRegisterDTO postRegisterDTO = new PostRegisterDTO(
                "Test Post",
                "This is a test post."
        );
        postService.createPost(1L, postRegisterDTO);

        CommentRegisterDTO commentRegisterDTO = new CommentRegisterDTO(
                null,
                "Initial comment"
        );
        commentService.createComment(commentRegisterDTO, 1L, 1L);

        CommentUpdateDTO commentUpdateDTO = new CommentUpdateDTO(
                1L,
                "Updated comment"
        );
        commentService.updateComment(commentUpdateDTO, 1L, 1L);

        assertEquals("Updated comment", commentService.viewComments(1L, 1L).getFirst().getContent());
    }

    @Test
    void testUpdateCommentFailWithUnauthorizedUser() throws Throwable {
        MemberLoginDTO loginDTO = new MemberLoginDTO("admin", "admin");
        memberAuthService.login(loginDTO);

        PostRegisterDTO postRegisterDTO = new PostRegisterDTO(
                "Test Post",
                "This is a test post."
        );
        postService.createPost(1L, postRegisterDTO);

        CommentRegisterDTO commentRegisterDTO = new CommentRegisterDTO(
                null,
                "Initial comment"
        );
        commentService.createComment(commentRegisterDTO, 1L, 1L);

        memberAuthService.logout();
        MemberRegisterDTO otherMemberRegisterDTO = new MemberRegisterDTO("user", "User Name", "user@example.com", "password");
        memberService.registerMember(otherMemberRegisterDTO);
        MemberLoginDTO otherMemberLoginDTO = new MemberLoginDTO("user", "password");
        memberAuthService.login(otherMemberLoginDTO);

        CommentUpdateDTO commentUpdateDTO = new CommentUpdateDTO(
                1L,
                "Unauthorized update"
        );

        assertThrows(UnauthorizedAccessException.class, () -> commentService.updateComment(commentUpdateDTO, 1L, 1L));
    }

    @Test
    void testDeleteCommentSuccessfully() throws Throwable {
        MemberLoginDTO loginDTO = new MemberLoginDTO("admin", "admin");
        memberAuthService.login(loginDTO);

        PostRegisterDTO postRegisterDTO = new PostRegisterDTO(
                "Test Post",
                "This is a test post."
        );
        postService.createPost(1L, postRegisterDTO);

        CommentRegisterDTO commentRegisterDTO = new CommentRegisterDTO(
                null,
                "Test comment"
        );
        commentService.createComment(commentRegisterDTO, 1L, 1L);

        commentService.deleteComment(1L, 1L, 1L);
        assertEquals(0, commentService.viewComments( 1L, 1L).size());
    }

    @Test
    void testDeleteCommentFailWithUnauthorizedUser() throws Throwable {
        MemberLoginDTO loginDTO = new MemberLoginDTO("admin", "admin");
        memberAuthService.login(loginDTO);

        PostRegisterDTO postRegisterDTO = new PostRegisterDTO(
                "Test Post",
                "This is a test post."
        );
        postService.createPost(1L, postRegisterDTO);

        CommentRegisterDTO commentRegisterDTO = new CommentRegisterDTO(
                null,
                "Test comment"
        );
        commentService.createComment(commentRegisterDTO, 1L, 1L);

        memberAuthService.logout();
        MemberRegisterDTO otherMemberRegisterDTO = new MemberRegisterDTO("user", "User Name", "user@example.com", "password");
        memberService.registerMember(otherMemberRegisterDTO);
        MemberLoginDTO otherMemberLoginDTO = new MemberLoginDTO("user", "password");
        memberAuthService.login(otherMemberLoginDTO);

        assertThrows(UnauthorizedAccessException.class, () -> commentService.deleteComment(1L, 1L, 1L));
    }

}
