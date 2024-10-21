package view;

public enum InputKeyword {
    EXIT("종료", "exit");

    private final String keyword;
    private final String keywordEn;

    InputKeyword(String keyword, String keywordEn) {
        this.keyword = keyword;
        this.keywordEn = keywordEn;
    }

    public String getKeyword() {
        return keyword;
    }

    public String getKeywordEn() {
        return keywordEn;
    }
}
