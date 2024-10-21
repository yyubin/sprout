package repository;

import config.annotations.Repository;
import domain.Post;
import exception.NotFoundPostWithPostIdException;
import message.ExceptionMessage;
import repository.interfaces.PostRepository;

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

    public void deleteById(Long postId) {
        findById(postId).ifPresent(post -> {
            post.setDeleted(true);
        });
    }

}
