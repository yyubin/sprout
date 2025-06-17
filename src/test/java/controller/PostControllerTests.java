//package controller;
//
//import app.controller.PostController;
//import app.domain.Post;
//import app.dto.PostRegisterDTO;
//import app.dto.PostUpdateDTO;
//import sprout.mvc.http.HttpResponse;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//
//import app.service.interfaces.PostServiceInterface;
//import legacy.view.interfaces.PrintProcessor;
//
//import java.time.LocalDateTime;
//
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.MockitoAnnotations;
//import static org.mockito.Mockito.*;
//
//
//public class PostControllerTests {
//
//    @Mock
//    private PostServiceInterface postService;
//
//    @Mock
//    private PrintProcessor printHandler;
//
//    @InjectMocks
//    private PostController postController;
//
//    @BeforeEach
//    public void setup() {
//        MockitoAnnotations.openMocks(this);
//    }
//
//    @Test
//    public void testAddPost() throws Throwable {
//        Long boardId = 1L; // Sample board ID
//        PostRegisterDTO postRegisterDTO = new PostRegisterDTO("Test Post", "This is a test post.");
//
//        postController.addPost(boardId, postRegisterDTO);
//
//        verify(postService, times(1)).createPost(eq(boardId), any(PostRegisterDTO.class));
//        verify(printHandler, times(1)).printSuccessWithResponseCodeAndCustomMessage(any(HttpResponse.class));
//    }
//
//    @Test
//    public void testRemovePost() throws Throwable {
//        Long boardId = 2L;
//        Long postId = 1L; // Sample post ID
//
//        postController.removePost(boardId, postId);
//
//        verify(postService, times(1)).deletePost(boardId, postId);
//        verify(printHandler, times(1)).printSuccessWithResponseCodeAndCustomMessage(any(HttpResponse.class));
//    }
//
//    @Test
//    public void testEditPost() throws Throwable {
//        Long boardId = 2L; // Sample board ID
//        Long postId = 1L; // Sample post ID
//        PostUpdateDTO postUpdateDTO = new PostUpdateDTO("Updated Post", "Updated Content"); // Adjusted constructor parameters
//
//        postController.editPost(boardId, postId, postUpdateDTO);
//
//        verify(postService, times(1)).updatePost(eq(boardId), eq(postId), any(PostUpdateDTO.class));
//        verify(printHandler, times(1)).printSuccessWithResponseCodeAndCustomMessage(any(HttpResponse.class));
//    }
//
//
//    @Test
//    public void testViewPost() {
//        Long boardId = 2L; // Sample board ID
//        Long postId = 1L; // Sample post ID
//
//        Post mockPost = mock(Post.class);
//        when(mockPost.getPostId()).thenReturn(postId);
//        when(mockPost.getCreatedDate()).thenReturn(LocalDateTime.now());
//        when(mockPost.getUpdatedDate()).thenReturn(LocalDateTime.now());
//        when(mockPost.getPostName()).thenReturn("Test Post");
//        when(mockPost.getPostContent()).thenReturn("Test Content");
//
//        when(postService.getPost(postId, boardId)).thenReturn(mockPost);
//
//        postController.viewPost(boardId, postId);
//
//        verify(printHandler, times(1)).printResponseBodyAsMap(any(HttpResponse.class));
//    }
//
//
//}
