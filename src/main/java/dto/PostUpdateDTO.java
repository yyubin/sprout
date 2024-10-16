package dto;

public class PostUpdateDTO {
    private Long postId;
    private String postName;
    private String postContent;

    public PostUpdateDTO(Long postId, String postName, String postContent) {
        this.postId = postId;
        this.postName = postName;
        this.postContent = postContent;
    }

    public Long getPostId() {
        return postId;
    }

    public String getPostName() {
        return postName;
    }

    public String getPostContent() {
        return postContent;
    }


}
