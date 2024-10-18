package config;

public enum Constants {
    path("path");

    private String constantsName;

    Constants(String constantsName) {
        this.constantsName = constantsName;
    }

    public String getConstantsName() {
        return constantsName;
    }
}
