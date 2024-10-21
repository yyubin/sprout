package service;

import config.Container;
import config.annotations.Priority;
import config.annotations.Requires;
import config.annotations.Service;
import domain.Board;
import domain.grade.MemberGrade;
import dto.BoardRegisterDTO;
import dto.BoardUpdateDTO;
import exception.BoardNameAlreadyExistsException;
import exception.NotFoundBoardWithBoardIdException;
import exception.UnauthorizedAccessException;
import message.ExceptionMessage;
import repository.InMemoryBoardRepository;
import repository.interfaces.BoardRepository;
import service.interfaces.BoardServiceInterface;
import util.Session;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

@Service
@Priority(value = 2)
@Requires(dependsOn = {InMemoryBoardRepository.class, MemberAuthService.class})
public class BoardService implements BoardServiceInterface {

    private final BoardRepository boardRepository;
    private final MemberAuthService memberAuthService;

    public BoardService(InMemoryBoardRepository boardRepository, MemberAuthService memberAuthService) {
        this.boardRepository = boardRepository;
        this.memberAuthService = memberAuthService;
    }

    public void createBoard(BoardRegisterDTO boardRegisterDTO) throws Throwable {
        checkAdminAuthority(Session.getSessionId());
        checkDuplicateBoardName(boardRegisterDTO.getBoardName());
        Board board = new Board(
                (long) (boardRepository.size() + 1),
                boardRegisterDTO.getBoardName(),
                boardRegisterDTO.getDescription(),
                boardRegisterDTO.getAccessGrades()
        );
        boardRepository.save(board);
    }

    public int getBoardSize() {
        return boardRepository.size();
    }

    public List<Board> getAllBoards() {
        return boardRepository.findAll();
    }

    public Optional<Board> getBoardById(Long boardId) throws NotFoundBoardWithBoardIdException {
        return Optional.ofNullable(boardRepository.findById(boardId)
                .orElseThrow(() -> new NotFoundBoardWithBoardIdException(ExceptionMessage.NOT_FOUND_BOARD_WITH_BOARD_ID, boardId)));
    }

    public void updateBoard(BoardUpdateDTO boardUpdateDTO) throws Throwable {
        checkAdminAuthority(Session.getSessionId());
        checkDuplicateBoardName(boardUpdateDTO.getBoardName());
        if (boardRepository.findById(boardUpdateDTO.getId()).isPresent()) {
            Board board = new Board(
                    boardUpdateDTO.getId(),
                    boardUpdateDTO.getBoardName(),
                    boardUpdateDTO.getDescription(),
                    boardUpdateDTO.getAccessGrades()
            );
            boardRepository.update(board);
            return;
        }
        throw new NotFoundBoardWithBoardIdException(ExceptionMessage.NOT_FOUND_BOARD_WITH_BOARD_ID, boardUpdateDTO.getId());
    }

    public void deleteBoard(Long boardId) throws Throwable {
        checkAdminAuthority(Session.getSessionId());
        if (boardRepository.findById(boardId).isPresent()) {
            boardRepository.delete(boardId);
        }
    }

    private void checkAdminAuthority(String sessionId) throws Throwable {
        MemberGrade memberGrade = memberAuthService.checkAuthority(sessionId);
        if (memberGrade != MemberGrade.ADMIN) {
            throw new UnauthorizedAccessException(ExceptionMessage.UNAUTHORIZED_CREATE_BOARD);
        }
    }

    private void checkDuplicateBoardName(String boardName) throws Throwable {
        checkExists(boardName,
                () -> boardRepository.existByName(boardName),
                () -> new BoardNameAlreadyExistsException(ExceptionMessage.ALREADY_USED_BOARD_NAME)
        );
    }

    private void checkExists(Object value, Supplier<Boolean> checkFunction, Supplier<Throwable> exceptionSupplier) throws Throwable {
        if (checkFunction.get()) {
            throw exceptionSupplier.get();
        }
    }
}
