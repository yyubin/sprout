package service.interfaces;

import dto.BoardRegisterDTO;
import dto.BoardUpdateDTO;

public interface BoardServiceInterface {

    void createBoard(BoardRegisterDTO boardRegisterDTO) throws Throwable;
    void updateBoard(Long boardId, BoardUpdateDTO boardUpdateDTO) throws Throwable;
    void deleteBoard(Long boardId) throws Throwable;

}
