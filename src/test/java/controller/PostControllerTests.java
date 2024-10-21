package controller;

import domain.Post;
import dto.PostRegisterDTO;
import dto.PostUpdateDTO;
import http.request.HttpRequest;
import http.response.HttpResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import service.PostService;
import view.PrintHandler;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.*;

public class PostControllerTests {

    @Mock
    private PostService postService;

    @Mock
    private PrintHandler printHandler;

    @InjectMocks
    private PostController postController;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testAddPost() {
        Map<String, Object> body = new HashMap<>();
        body.put("postName", "Test Post");
        body.put("postContent", "This is a test post.");

        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("boardId", "1");

        HttpRequest<Map<String, Object>> request = mock(HttpRequest.class);
        when(request.getBody()).thenReturn(body);
        when(request.getQueryParams()).thenReturn(queryParams);

        postController.addPost(request);

        verify(postService, times(1)).createPost(any(PostRegisterDTO.class));
        verify(printHandler, times(1)).printSuccessWithResponseCodeAndCustomMessage(any(HttpResponse.class));
    }

    @Test
    public void testRemovePost() {
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("postId", "1");

        HttpRequest<Map<String, Object>> request = mock(HttpRequest.class);
        when(request.getQueryParams()).thenReturn(queryParams);

        postController.removePost(request);

        verify(postService, times(1)).deletePost(1L);
        verify(printHandler, times(1)).printSuccessWithResponseCodeAndCustomMessage(any(HttpResponse.class));
    }

    @Test
    public void testEditPost() {
        Map<String, Object> body = new HashMap<>();
        body.put("postName", "Updated Post");
        body.put("postContent", "Updated Content");

        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("postId", "1");
        queryParams.put("boardId", "2");

        HttpRequest<Map<String, Object>> request = mock(HttpRequest.class);
        when(request.getBody()).thenReturn(body);
        when(request.getQueryParams()).thenReturn(queryParams);

        postController.editPost(request);

        verify(postService, times(1)).updatePost(any(PostUpdateDTO.class));
    }

    @Test
    public void testViewPost() {
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("postId", "1");
        queryParams.put("boardId", "2");

        Post mockPost = mock(Post.class);
        when(mockPost.getPostId()).thenReturn(1L);
        when(mockPost.getCreatedDate()).thenReturn(LocalDateTime.now());
        when(mockPost.getUpdatedDate()).thenReturn(LocalDateTime.now());
        when(mockPost.getPostName()).thenReturn("Test Post");
        when(mockPost.getPostContent()).thenReturn("Test Content");

        HttpRequest<Map<String, Object>> request = mock(HttpRequest.class);
        when(request.getQueryParams()).thenReturn(queryParams);

        when(postService.getPost(1L, 2L)).thenReturn(mockPost);

        postController.viewPost(request);

        verify(printHandler, times(1)).printResponseBodyAsMap(any(HttpResponse.class));
    }


}
