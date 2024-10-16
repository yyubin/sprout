package domain;

public enum MemberGrade {
    ADMIN("관리자", "admin"),
    USER("회원", "user");

    private final String description;
    private final String descriptionEn;

    MemberGrade(String description, String descriptionEn) {
        this.description = description;
        this.descriptionEn = descriptionEn;
    }

    public String getDescription() {
        return description;
    }

    public String getDescriptionEn() {
        return descriptionEn;
    }
}
