package domain;

import java.util.concurrent.atomic.AtomicLong;

public class Board {

    private Long boardId;
    private String boardName;
    private String description;
    private MemberGrade accessGrade;

    private AtomicLong postCounter;

    private boolean deleted;

    public Board(Long boardId, String boardName, String description, MemberGrade accessGrade, AtomicLong postCounter) {
        this.boardId = boardId;
        this.boardName = boardName;
        this.description = description;
        this.accessGrade = accessGrade;
        this.postCounter = new AtomicLong(0);
        this.deleted = false;
    }

    public Long generatedPostId() {
        return postCounter.incrementAndGet();
    }

}
