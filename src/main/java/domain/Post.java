package domain;

import java.time.LocalDateTime;

public class Post {

    private Long postId;
    private String postName;
    private String postContent;
    private Member author;
    private Board board;
    private LocalDateTime createdDate;
    private LocalDateTime updatedDate;
    private boolean deleted;

    public Post(String postName, String postContent, Member author, Board board, LocalDateTime createdDate) {
        this.postId = board.generatedPostId();
        this.postName = postName;
        this.postContent = postContent;
        this.author = author;
        this.board = board;
        this.createdDate = createdDate;
        this.updatedDate = null;
        this.deleted = false;
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

    public Member getAuthor() {
        return author;
    }

    public Board getBoard() {
        return board;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    public void setPostName(String postName) {
        this.postName = postName;
    }

    public void setPostContent(String postContent) {
        this.postContent = postContent;
    }

    public void setUpdatedDate(LocalDateTime updatedDate) {
        this.updatedDate = updatedDate;
    }
}
