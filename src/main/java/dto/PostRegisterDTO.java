package dto;

import domain.Board;

public class PostRegisterDTO {

    private String postName;
    private String postContent;

    public PostRegisterDTO(String postName, String postContent) {
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
