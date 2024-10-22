package repository.interfaces;

import domain.Post;

import java.util.List;
import java.util.Optional;

public interface PostRepository {

    void save(Post post);
    int allPostsSize();
    int postsSizeWithBoard(Long boardId);
    Optional<Post> findById(Long postId);
    Optional<Post> findByPostIdAndBoardId(Long postId, Long boardId);
    List<Post> findAll();
    List<Post> findPostsByName(String postName);
    List<Post> findPostsByAuthor(String author);
    List<Post> findPostsByBoardId(Long boardId);
    void update(Post post);
    void deleteById(Long boardId, Long postId);

}
