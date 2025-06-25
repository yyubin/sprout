//package app.controller;
//
//import com.fasterxml.jackson.core.JsonProcessingException;
//import sprout.beans.annotation.Controller;
//import sprout.beans.annotation.Requires;
//import sprout.mvc.annotation.DeleteMapping;
//import sprout.mvc.annotation.GetMapping;
//import sprout.mvc.annotation.PostMapping;
//import sprout.mvc.annotation.PutMapping;
//import app.domain.Post;
//import app.dto.PostRegisterDTO;
//import app.dto.PostUpdateDTO;
//import sprout.mvc.http.HttpResponse;
//import sprout.mvc.http.ResponseCode;
//import app.message.PrintResultMessage;
//import app.service.interfaces.PostServiceInterface;
//import sprout.mvc.mapping.ControllerInterface;
//
//import java.util.HashMap;
//import java.util.Map;
//
//@Controller
//@Requires(dependsOn = {PostServiceInterface.class})
//public class PostController implements ControllerInterface {
//
//    private final PostServiceInterface postService;
//
//    public PostController(PostServiceInterface postService) {
//        this.postService = postService;
//    }
//
//    @PostMapping(path = "/posts/add")
//    public HttpResponse<?> addPost(Long boardId, PostRegisterDTO postRegisterDTO) throws Throwable {;
//        postService.createPost(boardId, postRegisterDTO);
//        return new HttpResponse<>(
//                PrintResultMessage.POST_CREATE_SUCCESS,
//                ResponseCode.CREATED,
//                null
//        );
//    }
//
//    @DeleteMapping(path = "/posts/remove")
//    public HttpResponse<?> removePost(Long boardId, Long postId) throws Throwable {
//        postService.deletePost(boardId, postId);
//        return new HttpResponse<>(
//                PrintResultMessage.POST_DELETE_SUCCESS,
//                ResponseCode.ACCEPT,
//                null
//        );
//    }
//
//    @PutMapping(path = "/posts/edit")
//    public HttpResponse<?> editPost(Long boardId, Long postId, PostUpdateDTO postUpdateDTO) throws Throwable {
//        postService.updatePost(boardId, postId, postUpdateDTO);
//        return new HttpResponse<>(
//                PrintResultMessage.POST_UPDATE_SUCCESS,
//                ResponseCode.ACCEPT,
//                null
//        );
//    }
//
//    @GetMapping(path = "/posts/view")
//    public HttpResponse<?> viewPost(Long boardId, Long postId) throws JsonProcessingException {
//
//        Post post = postService.getPost(postId, boardId);
//
//        Map<String, Object> responseBody = new HashMap<>();
//        responseBody.put("게시글 번호", post.getPostId());
//        responseBody.put("작성일", post.getCreatedDate());
//        if (post.getUpdatedDate() != null) {
//            responseBody.put("수정일", post.getUpdatedDate());
//        } else {
//            responseBody.put("수정일", "수정 내역이 없습니다.");
//        }
//        responseBody.put("제목", post.getPostName());
//        responseBody.put("내용", post.getPostContent());
//
//        return new HttpResponse<>(
//                ResponseCode.SUCCESS.getMessage(), ResponseCode.SUCCESS, responseBody
//        );
//
//    }
//}
