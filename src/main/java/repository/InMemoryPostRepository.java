package repository;

import domain.Post;
import exception.NotFoundPostWithPostIdException;
import message.ExceptionMessage;

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

    public void update(Post post) {
        posts.set(posts.indexOf(post), post);
//        Optional<Post> existingPost = findById(post.getPostId());
//        existingPost.ifPresentOrElse(
//                p -> {
//                    int index = posts.indexOf(p);
//                    posts.set(index, post);
//                },
//                () -> {
//                    throw new NotFoundPostWithPostIdException(ExceptionMessage.NOT_FOUND_POST_WITH_POST_ID, post.getPostId());
//                }
//        );
    }

}
