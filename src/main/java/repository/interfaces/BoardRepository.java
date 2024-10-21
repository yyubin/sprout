package repository.interfaces;

import domain.Board;

import java.util.List;
import java.util.Optional;

public interface BoardRepository {

    int size();
    void save(Board board);
    Optional<Board> findById(Long id);
    List<Board> findAll();
    void update(Board board);
    void delete(Long boardId);
    Long findByName(String boardName);
    boolean existByName(String boardName);

}
