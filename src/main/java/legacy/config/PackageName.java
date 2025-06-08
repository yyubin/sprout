package legacy.config;

public enum PackageName {
    util("app/util"),
    config("legacy/config"),
    config_exception("config.exception"),
    domain("app/domain"),
    dto("app/dto"),
    repository("app/repository"),
    service("app/service"),
    controller("app/controller"),
    view("legacy/view"),
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
