package app.repository;

import sprout.beans.annotation.Repository;
import app.domain.Post;
import app.repository.interfaces.PostRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class InMemoryPostRepository implements PostRepository {
    private final List<Post> posts = new ArrayList<>();

    public void save(Post post) {
        posts.add(post);
    }

    public int allPostsSize() {
        return (int) posts.stream()
                .filter(post -> !post.isDeleted())
                .count();
    }

    public int postsSizeWithBoard(Long boardId) {
        return (int) posts.stream()
                .filter(post -> post.getBoard().getBoardId().equals(boardId) && !post.isDeleted())
                .count();
    }

    public Optional<Post> findById(Long postId) {
        return posts.stream()
                .filter(post -> post.getPostId().equals(postId) && !post.isDeleted())
                .findFirst();
    }

    public Optional<Post> findByPostIdAndBoardId(Long postId, Long boardId) {
        return posts.stream()
                .filter(post -> post.getPostId().equals(postId) && post.getBoard().getBoardId().equals(boardId) && !post.isDeleted())
                .findFirst();
    }

    public List<Post> findAll() {
        return posts.stream()
                .filter(post -> !post.isDeleted())
                .toList();
    }

    public List<Post> findPostsByName(String postName) {
        return posts.stream()
                .filter(post -> post.getPostName().equals(postName) && !post.isDeleted())
                .toList();
    }

    public List<Post> findPostsByAuthor(String author) {
        return posts.stream()
                .filter(post -> post.getAuthor().getName().equals(author) && !post.isDeleted())
                .toList();
    }

    public List<Post> findPostsByBoardId(Long boardId) {
        return posts.stream()
                .filter(post -> post.getBoard().getBoardId().equals(boardId) && !post.isDeleted())
                .toList();
    }

    public void update(Post post) {
        posts.set(posts.indexOf(post), post);
    }

    public void deleteById(Long boardId, Long postId) {
        findByPostIdAndBoardId(postId, boardId).ifPresent(post -> {
            post.setDeleted(true);
        });
    }

}
