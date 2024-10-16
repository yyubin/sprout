package domain;

import domain.grade.MemberGrade;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class Board {

    private Long boardId;
    private String boardName;
    private String description;
    private List<MemberGrade> accessGrade;

    private AtomicLong postCounter;
    private List<Post> posts;

    private boolean deleted;

    public Board(Long boardId, String boardName, String description, List<MemberGrade> accessGrade) {
        this.boardId = boardId;
        this.boardName = boardName;
        this.description = description;
        this.accessGrade = accessGrade;
        this.postCounter = new AtomicLong(0);
        this.posts = new ArrayList<>();
        this.deleted = false;
    }

    public Long generatedPostId() {
        return postCounter.incrementAndGet();
    }

    public Long getBoardId() {
        return boardId;
    }

    public List<MemberGrade> getAccessGrade() {
        return accessGrade;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }
}
