package service.interfaces;

import domain.Post;
import dto.PostRegisterDTO;
import dto.PostUpdateDTO;
import exception.MemberNotFoundException;
import exception.NotFoundBoardWithBoardIdException;
import exception.NotFoundPostWithPostIdException;
import exception.UnauthorizedAccessException;

import java.util.List;

public interface PostServiceInterface {
    void createPost(PostRegisterDTO postRegisterDTO) throws UnauthorizedAccessException, MemberNotFoundException, NotFoundBoardWithBoardIdException;

    void updatePost(PostUpdateDTO postUpdateDTO) throws UnauthorizedAccessException, NotFoundBoardWithBoardIdException, NotFoundPostWithPostIdException;

    void deletePost(Long postId) throws NotFoundPostWithPostIdException;

    List<Post> getPostsByBoardId(Long boardId) throws NotFoundBoardWithBoardIdException;

    List<Post> getAllPosts();

    List<Post> getPostsByMemberId(String authorName);

    List<Post> getPostsByPostName(String postName);

    List<Post> getPostsByBoardName(String boardName);

    Post getPost(Long postId, Long boardId) throws NotFoundPostWithPostIdException;

    int getAllPostsSize();

    int getPostsSizeWithBoardId(Long boardId);
}
