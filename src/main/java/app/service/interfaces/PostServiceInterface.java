package app.service.interfaces;

import app.domain.Post;
import app.dto.PostRegisterDTO;
import app.dto.PostUpdateDTO;
import exception.NotFoundBoardWithBoardIdException;
import exception.NotFoundPostWithPostIdException;

import java.util.List;

public interface PostServiceInterface {
    void createPost(Long boardId, PostRegisterDTO postRegisterDTO) throws Throwable;

    void updatePost(Long boardId, Long postId, PostUpdateDTO postUpdateDTO) throws Throwable;

    void deletePost(Long boardId, Long postId) throws Throwable;

    List<Post> getPostsByBoardId(Long boardId) throws NotFoundBoardWithBoardIdException;

    List<Post> getAllPosts();

    List<Post> getPostsByMemberId(String authorName);

    List<Post> getPostsByPostName(String postName);

    List<Post> getPostsByBoardName(String boardName);

    Post getPost(Long postId, Long boardId) throws NotFoundPostWithPostIdException;

    int getAllPostsSize();

    int getPostsSizeWithBoardId(Long boardId);
}
