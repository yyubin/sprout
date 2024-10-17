package repository;

import domain.Board;
import exception.NotFoundBoardWithBoardIdException;
import message.ExceptionMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

public class InMemoryBoardRepository {
    private final List<Board> boards = new ArrayList<>();

    public int size() {
        return (int) boards.stream()
                .filter(board -> !board.isDeleted())
                .count();
    }

    public void save(Board board) {
        boards.add(board);
    }

    public Optional<Board> findById(Long id) {
        return boards.stream()
                .filter(board -> board.getBoardId().equals(id) && !board.isDeleted())
                .findFirst();
    }

    public List<Board> findAll() {
        return boards.stream()
                .filter(board -> !board.isDeleted())
                .toList();
    }

    private int findBoardIndex(Long boardId) {
        return IntStream.range(0, boards.size())
                .filter(i -> boards.get(i).getBoardId().equals(boardId) && !boards.get(i).isDeleted())
                .findFirst()
                .orElseThrow(() -> new NotFoundBoardWithBoardIdException(ExceptionMessage.NOT_FOUND_BOARD_WITH_BOARD_ID, boardId));
    }

    public void update(Board board) {
        boards.set(findBoardIndex(board.getBoardId()), board);
    }

    public void delete(Long boardId) {
        boards.get(findBoardIndex(boardId)).setDeleted(true);
    }

    public boolean existByName(String boardName) {
        return boards.stream()
                .anyMatch(board -> board.getBoardName().equals(boardName) && !board.isDeleted());
    }
}
