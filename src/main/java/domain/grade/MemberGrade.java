package domain.grade;

import message.DescriptionMessage;

public enum MemberGrade {
    ADMIN(DescriptionMessage.ADMIN_DESCRIPTION, DescriptionMessage.ADMIN_DESCRIPTION_EN),
    USER(DescriptionMessage.USER_DESCRIPTION, DescriptionMessage.USER_DESCRIPTION_EN),;

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
