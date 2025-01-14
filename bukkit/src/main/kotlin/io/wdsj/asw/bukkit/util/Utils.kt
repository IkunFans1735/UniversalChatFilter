package io.wdsj.asw.bukkit.util

import com.github.houbb.heaven.util.lang.StringUtil
import com.google.common.util.concurrent.ThreadFactoryBuilder
import io.wdsj.asw.bukkit.AdvancedSensitiveWords
import io.wdsj.asw.bukkit.setting.PluginSettings
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicLong

object Utils {
    @JvmField
    val messagesFilteredNum: AtomicLong = AtomicLong(0)

    val commonWorker: ExecutorService = Executors.newCachedThreadPool(
        ThreadFactoryBuilder()
            .setNameFormat("ASW Common Worker-%d")
            .setPriority(Thread.NORM_PRIORITY - 1)
            .build()
    )

    @JvmStatic
    fun getPlayerIp(player: Player): String {
        val address = player.address
        if (address != null && address.address != null) return address.address.hostAddress
        throw IllegalStateException("Player address is null")
    }

    @JvmStatic
    fun isClassLoaded(className: String): Boolean {
        try {
            Class.forName(className)
            return true
        } catch (ignored: ClassNotFoundException) {
            return false
        }
    }

    fun isAnyClassLoaded(vararg classNames: String): Boolean {
        for (className in classNames) {
            if (isClassLoaded(className)) return true
        }
        return false
    }

    @JvmStatic
    fun isClassExists(className: String): Boolean {
        return try {
            val url = className.replace(".", "/") + ".class"
            return Thread.currentThread().contextClassLoader.getResource(url) != null
        } catch (ignored: Throwable) {
            false
        }
    }

    @JvmStatic
    fun canUsePE(): Boolean {
        return Bukkit.getPluginManager().getPlugin("packetevents") != null
    }
    fun isCommand(command: String): Boolean {
        return command.startsWith("/")
    }

    fun getSplitCommandArgs(command: String): String {
        val splitCommand = command.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        if (splitCommand.size <= 1) return ""
        return java.lang.String.join(" ", *Arrays.copyOfRange(splitCommand, 1, splitCommand.size))
    }

    fun getSplitCommandHeaders(command: String): String {
        val splitCommand = command.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        if (splitCommand.isEmpty()) return ""
        return splitCommand[0]
    }

    val preProcessRegex: String
        get() = AdvancedSensitiveWords.settingsManager.getProperty(PluginSettings.PRE_PROCESS_REGEX)


    fun isCommandAndWhiteListed(command: String): Boolean {
        if (!command.startsWith("/")) return false
        val whitelist = AdvancedSensitiveWords.settingsManager.getProperty(PluginSettings.CHAT_COMMAND_WHITE_LIST)
        val splitCommand = command.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        for (s in whitelist) {
            if (splitCommand[0].equals(s, ignoreCase = true)) {
                return !AdvancedSensitiveWords.settingsManager.getProperty(PluginSettings.CHAT_INVERT_WHITELIST)
            }
        }
        return AdvancedSensitiveWords.settingsManager.getProperty(PluginSettings.CHAT_INVERT_WHITELIST)
    }

    @JvmStatic
    val minecraftVersion: String
        get() = Bukkit.getBukkitVersion().split("-".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[0]

    fun isNotCommand(command: String): Boolean {
        return !command.startsWith("/")
    }

    /**
     * Checks if the given object is null, and returns the fallback value if it is.
     * @param obj The object to check
     * @param fallback The fallback value to return if the object is null
     */
    fun <T> checkNotNullWithFallback(obj: T?, fallback: T): T {
        if (obj == null) {
            return fallback
        }
        return obj
    }
}
