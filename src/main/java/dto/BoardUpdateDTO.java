package dto;

import domain.grade.MemberGrade;

import java.util.List;

public class BoardUpdateDTO {

    private Long id;
    private String boardName;
    private String description;
    private List<MemberGrade> accessGrades;

    public BoardUpdateDTO(Long id, String boardName, String description, List<MemberGrade> accessGrades) {
        this.id = id;
        this.boardName = boardName;
        this.description = description;
        this.accessGrades = accessGrades;
    }

    public Long getId() {
        return id;
    }

    public String getBoardName() {
        return boardName;
    }

    public String getDescription() {
        return description;
    }

    public List<MemberGrade> getAccessGrades() {
        return accessGrades;
    }
}
