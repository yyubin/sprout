package app.dto;

public class PostUpdateDTO {

    private String postName;
    private String postContent;

    public PostUpdateDTO() {
    }

    public PostUpdateDTO(String postName, String postContent) {
        this.postName = postName;
        this.postContent = postContent;
    }

    public String getPostName() {
        return postName;
    }

    public String getPostContent() {
        return postContent;
    }

    public void setPostName(String postName) {
        this.postName = postName;
    }

    public void setPostContent(String postContent) {
        this.postContent = postContent;
    }
}
