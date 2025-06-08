package app.dto;

public class BoardUpdateDTO {

    private Long id;
    private String boardName;
    private String description;
    private String grade;

    public BoardUpdateDTO() {
    }

    public BoardUpdateDTO(Long id, String boardName, String description, String grade) {
        this.id = id;
        this.boardName = boardName;
        this.description = description;
        this.grade = grade;
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

    public String getGrade() {
        return grade;
    }

    public void setId(Long id) {
        this.id = id;
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
