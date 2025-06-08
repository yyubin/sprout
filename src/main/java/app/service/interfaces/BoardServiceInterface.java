package app.service.interfaces;

import app.domain.Board;
import app.dto.BoardRegisterDTO;
import app.dto.BoardUpdateDTO;

import java.util.List;
import java.util.Optional;

public interface BoardServiceInterface {

    void createBoard(BoardRegisterDTO boardRegisterDTO) throws Throwable;
    void updateBoard(Long boardId, BoardUpdateDTO boardUpdateDTO) throws Throwable;
    void deleteBoard(Long boardId) throws Throwable;
    Optional<Board> getBoardById(Long boardId) throws Throwable;
    int getBoardSize();
    List<Board> getAllBoards();
}
