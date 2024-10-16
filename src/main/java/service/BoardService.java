package service;

import domain.Board;
import domain.grade.MemberGrade;
import dto.BoardRegisterDTO;
import dto.BoardUpdateDTO;
import exception.NotFoundBoardWithBoardIdException;
import exception.UnauthorizedAccessException;
import message.ExceptionMessage;
import repository.InMemoryBoardRepository;

import java.util.List;

public class BoardService {

    private final InMemoryBoardRepository boardRepository;
    private final MemberAuthService memberAuthService;

    public BoardService(InMemoryBoardRepository boardRepository, MemberAuthService memberAuthService) {
        this.boardRepository = boardRepository;
        this.memberAuthService = memberAuthService;
    }

    public void createBoard(BoardRegisterDTO boardRegisterDTO, String sessionId) throws UnauthorizedAccessException {
        checkAuthority(sessionId);
        Board board = new Board(
                (long) (boardRepository.size() + 1),
                boardRegisterDTO.getBoardName(),
                boardRegisterDTO.getDescription(),
                boardRegisterDTO.getAccessGrades()
        );
        boardRepository.save(board);
    }

    public List<Board> getAllBoards() {
        return boardRepository.findAll();
    }

    public Board getBoardById(long boardId) throws NotFoundBoardWithBoardIdException {
        return boardRepository.findById(boardId)
                .orElseThrow(() -> new NotFoundBoardWithBoardIdException(ExceptionMessage.NOT_FOUND_BOARD_WITH_BOARD_ID, boardId));
    }

    public void updateBoard(BoardUpdateDTO boardUpdateDTO, String sessionId) throws UnauthorizedAccessException, NotFoundBoardWithBoardIdException {
        checkAuthority(sessionId);
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

    public void deleteBoard(long boardId, String sessionId) throws NotFoundBoardWithBoardIdException {
        checkAuthority(sessionId);
        if (boardRepository.findById(boardId).isPresent()) {
            boardRepository.delete(boardId);
        }
    }

    private void checkAuthority(String sessionId) throws UnauthorizedAccessException {
        MemberGrade memberGrade = memberAuthService.checkAuthority(sessionId);
        if (memberGrade != MemberGrade.ADMIN) {
            throw new UnauthorizedAccessException(ExceptionMessage.UNAUTHORIZED_CREATE_BOARD);
        }
    }
}
