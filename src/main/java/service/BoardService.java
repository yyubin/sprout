package service;

import config.Container;
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
import util.Session;

import java.util.List;
import java.util.Optional;

@Service
public class BoardService {

    private final InMemoryBoardRepository boardRepository;
    private final MemberAuthService memberAuthService;

    public BoardService() {
        this.boardRepository = Container.getInstance().get(InMemoryBoardRepository.class);
        this.memberAuthService = Container.getInstance().get(MemberAuthService.class);
    }

    public void createBoard(BoardRegisterDTO boardRegisterDTO) throws UnauthorizedAccessException, BoardNameAlreadyExistsException {
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

    public void updateBoard(BoardUpdateDTO boardUpdateDTO) throws UnauthorizedAccessException, NotFoundBoardWithBoardIdException, BoardNameAlreadyExistsException {
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

    public void deleteBoard(Long boardId) throws NotFoundBoardWithBoardIdException {
        checkAdminAuthority(Session.getSessionId());
        if (boardRepository.findById(boardId).isPresent()) {
            boardRepository.delete(boardId);
        }
    }

    private void checkAdminAuthority(String sessionId) throws UnauthorizedAccessException {
        MemberGrade memberGrade = memberAuthService.checkAuthority(sessionId);
        if (memberGrade != MemberGrade.ADMIN) {
            throw new UnauthorizedAccessException(ExceptionMessage.UNAUTHORIZED_CREATE_BOARD);
        }
    }

    private void checkDuplicateBoardName(String boardName) throws BoardNameAlreadyExistsException {
        if (boardRepository.existByName(boardName)) {
            throw new BoardNameAlreadyExistsException(ExceptionMessage.ALREADY_USED_BOARD_NAME);
        }
    }
}
