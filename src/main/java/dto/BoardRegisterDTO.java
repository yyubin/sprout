package dto;

import domain.grade.MemberGrade;

import java.util.List;

public class BoardRegisterDTO {

    private String boardName;
    private String description;
    private List<MemberGrade> accessGrades;

    public BoardRegisterDTO(String boardName, String description, List<MemberGrade> accessGrades) {
        this.boardName = boardName;
        this.description = description;
        this.accessGrades = accessGrades;
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
