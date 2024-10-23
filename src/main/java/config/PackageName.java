package config;

public enum PackageName {
    util("util"),
    config("config"),
    config_exception("config.exception"),
    domain("domain"),
    dto("dto"),
    repository("repository"),
    service("service"),
    controller("controller"),
    view("view"),
    http_request("http.request"),
    ;

    private String packageName;

    PackageName(String packageName) {
        this.packageName = packageName;
    }

    public String getPackageName() {
        return packageName;
    }
}
