package dto;

import domain.Board;

public class PostRegisterDTO {

    private String postName;
    private String postContent;
    private Long boardId;

    public PostRegisterDTO(String postName, String postContent, Long boardId) {
        this.postName = postName;
        this.postContent = postContent;
        this.boardId = boardId;
    }

    public String getPostName() {
        return postName;
    }

    public String getPostContent() {
        return postContent;
    }

    public Long getBoardId() {
        return boardId;
    }
}
