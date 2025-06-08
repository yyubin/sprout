package app.dto;

public class BoardRegisterDTO {

    private String boardName;
    private String description;
    private String grade;

    public BoardRegisterDTO() {
    }

    public BoardRegisterDTO(String boardName, String description, String grade) {
        this.boardName = boardName;
        this.description = description;
        this.grade = grade;
    }

    public String getBoardName() {
        return boardName;
    }

    public String getDescription() {
        return description;
    }


    public String getGrade() {
        return grade;
    }

    public void setBoardName(String boardName) {
        this.boardName = boardName;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setGrade(String grade) {
        this.grade = grade;
    }

}
