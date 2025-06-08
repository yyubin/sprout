package app.dto;

public class CommentRegisterDTO {
    private Long parentCommentId;
    private String content;

    public CommentRegisterDTO() {
    }

    public CommentRegisterDTO(Long parentCommentId, String content) {
        this.parentCommentId = parentCommentId;
        this.content = content;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Long getParentCommentId() {
        return parentCommentId;
    }

    public void setParentCommentId(Long parentCommentId) {
        this.parentCommentId = parentCommentId;
    }
}
