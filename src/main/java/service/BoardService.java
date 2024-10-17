package service;

import domain.Board;
import domain.grade.MemberGrade;
import dto.BoardRegisterDTO;
import dto.BoardUpdateDTO;
import exception.NotFoundBoardWithBoardIdException;
import exception.UnauthorizedAccessException;
import message.ExceptionMessage;
import repository.InMemoryBoardRepository;
import util.Session;

import java.util.List;
import java.util.Optional;

public class BoardService {

    private final InMemoryBoardRepository boardRepository;
    private final MemberAuthService memberAuthService;

    public BoardService(InMemoryBoardRepository boardRepository, MemberAuthService memberAuthService) {
        this.boardRepository = boardRepository;
        this.memberAuthService = memberAuthService;
    }

    public void createBoard(BoardRegisterDTO boardRegisterDTO) throws UnauthorizedAccessException {
        checkAdminAuthority(Session.getSessionId());
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

    public Optional<Board> getBoardById(long boardId) throws NotFoundBoardWithBoardIdException {
        return Optional.ofNullable(boardRepository.findById(boardId)
                .orElseThrow(() -> new NotFoundBoardWithBoardIdException(ExceptionMessage.NOT_FOUND_BOARD_WITH_BOARD_ID, boardId)));
    }

    public void updateBoard(BoardUpdateDTO boardUpdateDTO) throws UnauthorizedAccessException, NotFoundBoardWithBoardIdException {
        checkAdminAuthority(Session.getSessionId());
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
}
