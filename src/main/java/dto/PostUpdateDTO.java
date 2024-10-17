package dto;

public class PostUpdateDTO {
    private Long postId;
    private Long boardId;
    private String postName;
    private String postContent;

    public PostUpdateDTO(Long postId, Long boardId, String postName, String postContent) {
        this.postId = postId;
        this.boardId = boardId;
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

    public Long getBoardId() {
        return boardId;
    }
}
