package service;

import com.sun.tools.javac.Main;
import config.Container;
import config.PackageName;
import domain.Board;
import domain.Member;
import domain.grade.MemberGrade;
import dto.BoardRegisterDTO;
import dto.BoardUpdateDTO;
import dto.MemberLoginDTO;
import dto.MemberRegisterDTO;
import exception.BoardNameAlreadyExistsException;
import exception.NotFoundBoardWithBoardIdException;
import exception.UnauthorizedAccessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import repository.InMemoryBoardRepository;
import repository.InMemoryMemberRepository;
import service.interfaces.BoardServiceInterface;
import service.interfaces.MemberAuthServiceInterface;
import service.interfaces.MemberServiceInterface;
import util.RedisSessionManager;
import util.Session;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class BoardServiceTests {

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
    }



    @Test
    @DisplayName("어드민 계정으로 계시판 만들고 성공해야함")
    void createBoardWithAdminSuccess() throws Throwable {
        // given
        memberService.registerAdminMember();
        MemberLoginDTO loginDTO = new MemberLoginDTO("admin", "admin");
        BoardRegisterDTO boardRegisterDTO = new BoardRegisterDTO(
                "Test Baord",
                "This is test-board",
                "USER"
        );

        memberAuthService.login(loginDTO);
        // when
        boardService.createBoard(boardRegisterDTO);

        //then
        assertEquals(1, boardService.getBoardSize());
    }

    @Test
    @DisplayName("멤버 계정으로 게시판 만들면 실패해야함")
    void createBoardWithMemberFail() throws Throwable {
        // given
        MemberRegisterDTO memberDTO = new MemberRegisterDTO("yubin111", "yubin", "yubin@gmail.com", "qwer");
        memberService.registerMember(memberDTO);

        MemberLoginDTO memberLoginDTO = new MemberLoginDTO("yubin111", "qwer");
        memberAuthService.login(memberLoginDTO);

        BoardRegisterDTO boardRegisterDTO = new BoardRegisterDTO(
                "Test Baord",
                "This is test-board",
                "USER"
        );

        // then & when
        assertThrows(UnauthorizedAccessException.class, () -> boardService.createBoard(boardRegisterDTO));

        // @springboottest
        // assertj

    }

    @Test
    void updateBoardWithAdminSuccess() throws Throwable {
        memberService.registerAdminMember();
        MemberLoginDTO loginDTO = new MemberLoginDTO("admin", "admin");
        BoardRegisterDTO boardRegisterDTO = new BoardRegisterDTO(
                "Test Baord",
                "This is test-board",
                "USER"
        );
        String sessionId = memberAuthService.login(loginDTO);
        boardService.createBoard(boardRegisterDTO);

        BoardUpdateDTO boardUpdateDTO = new BoardUpdateDTO(
                1L,
                "Test Board2",
                "Update board description",
                "USER"
        );
        boardService.updateBoard(1L, boardUpdateDTO);
        Optional<Board> board = boardService.getBoardById(1L);
        assertTrue(board.isPresent());
        assertEquals("Test Board2", board.get().getBoardName());
        assertEquals("Update board description", board.get().getDescription());
    }

    @Test
    void updateBoardWithMemberFail() throws Throwable {
        MemberRegisterDTO memberDTO = new MemberRegisterDTO("yubin111", "yubin", "yubin@gmail.com", "qwer");
        memberService.registerMember(memberDTO);

        MemberLoginDTO memberLoginDTO = new MemberLoginDTO("yubin111", "qwer");
        memberAuthService.login(memberLoginDTO);

        BoardUpdateDTO boardUpdateDTO = new BoardUpdateDTO(
                1L,
                "Test Board2",
                "Update board description",
                "ADMIN"
        );

        assertThrows(UnauthorizedAccessException.class, () -> boardService.updateBoard(1L, boardUpdateDTO));
    }

    @Test
    void deleteBoardWithAdminSuccess() throws Throwable {
        memberService.registerAdminMember();
        MemberLoginDTO loginDTO = new MemberLoginDTO("admin", "admin");
        BoardRegisterDTO boardRegisterDTO = new BoardRegisterDTO(
                "Test Baord",
                "This is test-board",
                "USER"
        );
        String sessionId = memberAuthService.login(loginDTO);
        boardService.createBoard(boardRegisterDTO);
        boardService.deleteBoard(1L);
        assertEquals(0, boardService.getBoardSize());
    }

    @Test
    void deleteBoardWithMemberFail() throws Throwable {
        MemberRegisterDTO memberDTO = new MemberRegisterDTO("yubin111", "yubin", "yubin@gmail.com", "qwer");
        memberService.registerMember(memberDTO);

        MemberLoginDTO memberLoginDTO = new MemberLoginDTO("yubin111", "qwer");
        memberAuthService.login(memberLoginDTO);

        assertThrows(UnauthorizedAccessException.class, () -> boardService.deleteBoard(1L));
    }

    @Test
    void getAllBoards() throws Throwable {
        memberService.registerAdminMember();
        MemberLoginDTO loginDTO = new MemberLoginDTO("admin", "admin");
        String sessionId = memberAuthService.login(loginDTO);
        BoardRegisterDTO boardRegisterDTO1 = new BoardRegisterDTO(
                "Test Baord1",
                "This is test-board",
                "USER"
        );
        BoardRegisterDTO boardRegisterDTO2 = new BoardRegisterDTO(
                "Test Baord2",
                "This is test-board",
                "USER"
        );
        boardService.createBoard(boardRegisterDTO1);
        boardService.createBoard(boardRegisterDTO2);
        List<Board> boards = boardService.getAllBoards();
        assertEquals(2, boards.size());
    }

    @Test
    void testDuplicateBoardNameFail() throws Throwable {
        memberService.registerAdminMember();
        MemberLoginDTO loginDTO = new MemberLoginDTO("admin", "admin");
        String sessionId = memberAuthService.login(loginDTO);
        BoardRegisterDTO boardRegisterDTO1 = new BoardRegisterDTO(
                "Test Baord",
                "This is test-board",
                "USER"
        );
        BoardRegisterDTO boardRegisterDTO2 = new BoardRegisterDTO(
                "Test Baord",
                "This is test-board",
                "USER"
        );
        boardService.createBoard(boardRegisterDTO1);
        assertThrows(BoardNameAlreadyExistsException.class, () -> boardService.createBoard(boardRegisterDTO2));
    }
}
