package app.dto;

public class CommentUpdateDTO {
    private Long commentId;
    private String content;

    public CommentUpdateDTO() {
    }

    public CommentUpdateDTO(Long commentId, String content) {
        this.commentId = commentId;
        this.content = content;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Long getCommentId() {
        return commentId;
    }

    public void setCommentId(Long commentId) {
        this.commentId = commentId;
    }
}
