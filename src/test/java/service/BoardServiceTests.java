package service;

import domain.Board;
import domain.Member;
import domain.grade.MemberGrade;
import dto.BoardRegisterDTO;
import dto.BoardUpdateDTO;
import dto.MemberLoginDTO;
import dto.MemberRegisterDTO;
import exception.NotFoundBoardWithBoardIdException;
import exception.UnauthorizedAccessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import repository.InMemoryBoardRepository;
import util.RedisSessionManager;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class BoardServiceTests {

    private InMemoryBoardRepository boardRepository;
    private MemberAuthService memberAuthService;
    private BoardService boardService;
    private MemberService memberService;

    @BeforeEach
    void setUp() {
        RedisSessionManager redisSessionManager = new RedisSessionManager();
        memberService = new MemberService();
        boardRepository = new InMemoryBoardRepository();
        memberAuthService = new MemberAuthService(memberService, redisSessionManager);
        boardService = new BoardService(boardRepository, memberAuthService);
    }

    @Test
    void createBoardWithAdminSuccess() throws UnauthorizedAccessException {
        memberService.registerAdminMember();
        MemberLoginDTO loginDTO = new MemberLoginDTO("admin", "admin");
        BoardRegisterDTO boardRegisterDTO = new BoardRegisterDTO(
                "Test Baord",
                "This is test-board",
                List.of(MemberGrade.ADMIN, MemberGrade.USER)
        );
        memberAuthService.login(loginDTO);
        boardService.createBoard(boardRegisterDTO);
        assertEquals(1, boardService.getBoardSize());
    }

    @Test
    void createBoardWithMemberFail() throws UnauthorizedAccessException {
        MemberRegisterDTO memberDTO = new MemberRegisterDTO("yubin111", "yubin", "yubin@gmail.com", "qwer", LocalDate.now());
        memberService.registerMember(memberDTO);

        MemberLoginDTO memberLoginDTO = new MemberLoginDTO("yubin111", "qwer");
        memberAuthService.login(memberLoginDTO);

        BoardRegisterDTO boardRegisterDTO = new BoardRegisterDTO(
                "Test Baord",
                "This is test-board",
                List.of(MemberGrade.ADMIN, MemberGrade.USER)
        );

        assertThrows(UnauthorizedAccessException.class, () -> boardService.createBoard(boardRegisterDTO));
    }

    @Test
    void updateBoardWithAdminSuccess() throws UnauthorizedAccessException, NotFoundBoardWithBoardIdException {
        memberService.registerAdminMember();
        MemberLoginDTO loginDTO = new MemberLoginDTO("admin", "admin");
        BoardRegisterDTO boardRegisterDTO = new BoardRegisterDTO(
                "Test Baord",
                "This is test-board",
                List.of(MemberGrade.ADMIN, MemberGrade.USER)
        );
        String sessionId = memberAuthService.login(loginDTO);
        boardService.createBoard(boardRegisterDTO);

        BoardUpdateDTO boardUpdateDTO = new BoardUpdateDTO(
                1L,
                "Test Board2",
                "Update board description",
                List.of(MemberGrade.ADMIN)
        );
        boardService.updateBoard(boardUpdateDTO);
        Optional<Board> board = boardService.getBoardById(1L);
        assertTrue(board.isPresent());
        assertEquals("Test Board2", board.get().getBoardName());
        assertEquals("Update board description", board.get().getDescription());
    }

    @Test
    void updateBoardWithMemberFail() throws UnauthorizedAccessException, NotFoundBoardWithBoardIdException {
        MemberRegisterDTO memberDTO = new MemberRegisterDTO("yubin111", "yubin", "yubin@gmail.com", "qwer", LocalDate.now());
        memberService.registerMember(memberDTO);

        MemberLoginDTO memberLoginDTO = new MemberLoginDTO("yubin111", "qwer");
        memberAuthService.login(memberLoginDTO);

        BoardUpdateDTO boardUpdateDTO = new BoardUpdateDTO(
                1L,
                "Test Board2",
                "Update board description",
                List.of(MemberGrade.ADMIN)
        );

        assertThrows(UnauthorizedAccessException.class, () -> boardService.updateBoard(boardUpdateDTO));
    }

    @Test
    void deleteBoardWithAdminSuccess() throws UnauthorizedAccessException, NotFoundBoardWithBoardIdException {
        memberService.registerAdminMember();
        MemberLoginDTO loginDTO = new MemberLoginDTO("admin", "admin");
        BoardRegisterDTO boardRegisterDTO = new BoardRegisterDTO(
                "Test Baord",
                "This is test-board",
                List.of(MemberGrade.ADMIN, MemberGrade.USER)
        );
        String sessionId = memberAuthService.login(loginDTO);
        boardService.createBoard(boardRegisterDTO);
        boardService.deleteBoard(1L);
        assertEquals(0, boardService.getBoardSize());
    }

    @Test
    void deleteBoardWithMemberFail() throws UnauthorizedAccessException, NotFoundBoardWithBoardIdException {
        MemberRegisterDTO memberDTO = new MemberRegisterDTO("yubin111", "yubin", "yubin@gmail.com", "qwer", LocalDate.now());
        memberService.registerMember(memberDTO);

        MemberLoginDTO memberLoginDTO = new MemberLoginDTO("yubin111", "qwer");
        memberAuthService.login(memberLoginDTO);

        assertThrows(UnauthorizedAccessException.class, () -> boardService.deleteBoard(1L));
    }

    @Test
    void getAllBoards() {
        memberService.registerAdminMember();
        MemberLoginDTO loginDTO = new MemberLoginDTO("admin", "admin");
        String sessionId = memberAuthService.login(loginDTO);
        BoardRegisterDTO boardRegisterDTO1 = new BoardRegisterDTO(
                "Test Baord1",
                "This is test-board",
                List.of(MemberGrade.ADMIN, MemberGrade.USER)
        );
        BoardRegisterDTO boardRegisterDTO2 = new BoardRegisterDTO(
                "Test Baord2",
                "This is test-board",
                List.of(MemberGrade.ADMIN, MemberGrade.USER)
        );
        boardService.createBoard(boardRegisterDTO1);
        boardService.createBoard(boardRegisterDTO2);
        List<Board> boards = boardService.getAllBoards();
        assertEquals(2, boards.size());
    }


}
