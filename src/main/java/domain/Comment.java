package domain;

import java.time.LocalDateTime;

public class Comment {
    private Long id;
    private Long boardId;
    private Long postId;
    private Long parentCommentId;
    private String author;
    private String content;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean deleted;

    public Comment(Long id, Long boardId, Long postId, Long parentCommentId, String author, String content, LocalDateTime createdAt) {
        this.id = id;
        this.boardId = boardId;
        this.postId = postId;
        this.parentCommentId = parentCommentId;
        this.author = author;
        this.content = content;
        this.createdAt = createdAt;
        this.deleted = false;
    }

    public Long getId() {
        return id;
    }

    public Long getPostId() {
        return postId;
    }

    public Long getParentCommentId() {
        return parentCommentId;
    }

    public String getAuthor() {
        return author;
    }

    public String getContent() {
        return content;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public Long getBoardId() {
        return boardId;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }
}
