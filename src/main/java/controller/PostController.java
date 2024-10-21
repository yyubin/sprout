package controller;

import config.annotations.Controller;
import config.annotations.Requires;
import controller.annotations.DeleteMapping;
import controller.annotations.GetMapping;
import controller.annotations.PostMapping;
import controller.annotations.PutMapping;
import domain.Post;
import dto.PostRegisterDTO;
import dto.PostUpdateDTO;
import http.request.HttpRequest;
import http.response.HttpResponse;
import http.response.ResponseCode;
import message.PrintResultMessage;
import service.PostService;
import view.PrintHandler;

import java.util.HashMap;
import java.util.Map;

@Controller
@Requires(dependsOn = {PostService.class, PrintHandler.class})
public class PostController implements ControllerInterface{

    private final PostService postService;
    private final PrintHandler printHandler;

    public PostController(PostService postService, PrintHandler printHandler) {
        this.postService = postService;
        this.printHandler = printHandler;
    }

    @PostMapping(path = "/posts/add")
    public void addPost(HttpRequest<Map<String, Object>> request) {
        Map<String, Object> body = request.getBody();
        Map<String, String> queryParams = request.getQueryParams();
        Long boardId = Long.valueOf(queryParams.get("boardId"));
        PostRegisterDTO registerDTO = new PostRegisterDTO(
                (String) body.get("postName"),
                (String) body.get("postContent"),
                boardId
        );
        postService.createPost(registerDTO);

        HttpResponse<?> response = new HttpResponse<>(
                PrintResultMessage.POST_CREATE_SUCCESS,
                ResponseCode.SUCCESS,
                null
        );
        printHandler.printSuccessWithResponseCodeAndCustomMessage(response);
    }

    @DeleteMapping(path = "/posts/remove")
    public void removePost(HttpRequest<Map<String, Object>> request) {
        Map<String, String> queryParams = request.getQueryParams();
        Long postId = Long.valueOf(queryParams.get("postId"));
        postService.deletePost(postId);
        HttpResponse<?> response = new HttpResponse<>(
                PrintResultMessage.POST_DELETE_SUCCESS,
                ResponseCode.SUCCESS,
                null
        );
        printHandler.printSuccessWithResponseCodeAndCustomMessage(response);
    }

    @PutMapping(path = "/posts/edit")
    public void editPost(HttpRequest<Map<String, Object>> request) {
        Map<String, Object> body = request.getBody();
        Map<String, String> queryParams = request.getQueryParams();
        Long postId = Long.valueOf(queryParams.get("postId"));
        PostUpdateDTO updateDTO = new PostUpdateDTO(
                postId,
                Long.valueOf((String) body.get("boardId")),
                (String) body.get("postName"),
                (String) body.get("postContent")
        );
        postService.updatePost(updateDTO);
    }

    @GetMapping(path = "/posts/view")
    public void viewPost(HttpRequest<Map<String, Object>> request) {
        Map<String, String> queryParams = request.getQueryParams();
        Post post = postService.getPost(Long.valueOf(queryParams.get("postId")), Long.valueOf(queryParams.get("boardId")));

        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("게시글 번호", post.getPostId());
        responseBody.put("작성일", post.getCreatedDate());
        responseBody.put("수정일", post.getUpdatedDate());
        responseBody.put("제목", post.getPostName());
        responseBody.put("내용", post.getPostContent());

        HttpResponse<Map<String, Object>> response = new HttpResponse<>(
                ResponseCode.SUCCESS.getMessage(), ResponseCode.SUCCESS, responseBody
        );

        printHandler.printResponseBodyAsMap(response);
    }
}
