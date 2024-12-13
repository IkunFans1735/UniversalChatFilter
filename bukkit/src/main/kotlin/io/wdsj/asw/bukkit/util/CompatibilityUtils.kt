package io.wdsj.asw.bukkit.util

object CompatibilityUtils {
    @JvmStatic
    fun isHigherThan(major: Int, minor: Int): Boolean {
        val version = Utils.minecraftVersion.split(".")
        return version[0].toInt() > major || (version[0].toInt() == major && version[1].toInt() > minor)
    }

    @JvmStatic
    fun isHigherThanOrEqualTo(major: Int, minor: Int): Boolean {
        val version = Utils.minecraftVersion.split(".")
        return version[0].toInt() > major || (version[0].toInt() == major && version[1].toInt() >= minor)
    }

    @JvmStatic
    fun isHigherThan(major: Int): Boolean {
        val version = Utils.minecraftVersion.split(".")
        return version[0].toInt() > major
    }
}