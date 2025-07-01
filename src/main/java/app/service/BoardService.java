package app.service;

import app.domain.Board;
import app.domain.grade.MemberGrade;
import app.dto.BoardRegisterDTO;
import app.dto.BoardUpdateDTO;
import app.exception.BoardNameAlreadyExistsException;
import app.exception.NotFoundBoardWithBoardIdException;
import app.message.ExceptionMessage;
import app.repository.interfaces.BoardRepository;
import app.service.interfaces.BoardServiceInterface;
import legacy.config.annotations.Priority;
import legacy.BeforeAuthCheck;
import sprout.beans.annotation.Requires;
import sprout.beans.annotation.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

@Service
@Priority(value = 2)
@Requires(dependsOn = {BoardRepository.class})
@BeforeAuthCheck
public class BoardService implements BoardServiceInterface {

    private final BoardRepository boardRepository;

    public BoardService(BoardRepository boardRepository) {
        this.boardRepository = boardRepository;
    }

    @BeforeAuthCheck
    public void createBoard(BoardRegisterDTO boardRegisterDTO) throws Throwable {
        checkDuplicateBoardName(boardRegisterDTO.getBoardName());
        List<MemberGrade> gradeList = new ArrayList<>();
        gradeList.add(MemberGrade.ADMIN);

        if (isUserGrade(boardRegisterDTO.getGrade())) {
            gradeList.add(MemberGrade.USER);
        }

        Board board = new Board(
                (long) (boardRepository.size() + 1),
                boardRegisterDTO.getBoardName(),
                boardRegisterDTO.getDescription(),
                gradeList
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

    @BeforeAuthCheck
    public void updateBoard(Long boardId, BoardUpdateDTO boardUpdateDTO) throws Throwable {
        checkDuplicateBoardName(boardUpdateDTO.getBoardName());
        List<MemberGrade> gradeList = new ArrayList<>();
        gradeList.add(MemberGrade.ADMIN);

        if (isUserGrade(boardUpdateDTO.getGrade())) {
            gradeList.add(MemberGrade.USER);
        }

        if (boardRepository.findById(boardId).isPresent()) {
            Board board = new Board(
                    boardId,
                    boardUpdateDTO.getBoardName(),
                    boardUpdateDTO.getDescription(),
                    gradeList
            );
            boardRepository.update(board);
            return;
        }
        throw new NotFoundBoardWithBoardIdException(ExceptionMessage.NOT_FOUND_BOARD_WITH_BOARD_ID, boardUpdateDTO.getId());
    }

    @BeforeAuthCheck
    public void deleteBoard(Long boardId) throws Throwable {
        if (boardRepository.findById(boardId).isPresent()) {
            boardRepository.delete(boardId);
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

    private boolean isUserGrade(String grade) {
        return grade.equals(MemberGrade.USER.getDescription()) ||
                grade.equals(MemberGrade.USER.getDescriptionEn());
    }
}
