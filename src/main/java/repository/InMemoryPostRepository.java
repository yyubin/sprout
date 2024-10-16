package repository;

import domain.Post;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class InMemoryPostRepository {
    private final List<Post> posts = new ArrayList<>();

    public void save(Post post) {
        posts.add(post);
    }

    public Optional<Post> findById(Long postId) {
        return posts.stream()
                .filter(post -> post.getPostId().equals(postId))
                .findFirst();
    }

    public List<Post> findAll() {
        return new ArrayList<>(posts);
    }

    public void update() {

    }

}
