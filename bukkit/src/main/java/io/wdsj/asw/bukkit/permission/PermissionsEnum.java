package io.wdsj.asw.bukkit.permission;

/**
 * Permission enums
 */
public enum PermissionsEnum {
    BYPASS("bypass"),
    RELOAD("reload"),
    ADD("add"),
    REMOVE("remove"),
    STATUS("status"),
    TEST("test"),
    HELP("help"),
    NOTICE("notice"),
    INFO("info"),
    RESET("reset"),
    UPDATE("update"),
    PUNISH("punish");

    private final String permission;

    PermissionsEnum(String permission) {
        this.permission = permission;
    }

    public String getPermission() {
        return PREFIX + permission;
    }

    private static final String PREFIX = "advancedsensitivewords.";
}
