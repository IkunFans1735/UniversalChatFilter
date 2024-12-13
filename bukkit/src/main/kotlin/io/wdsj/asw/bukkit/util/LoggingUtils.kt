package io.wdsj.asw.bukkit.util

import com.github.houbb.heaven.util.io.FileUtil
import com.google.common.util.concurrent.ThreadFactoryBuilder
import io.wdsj.asw.bukkit.AdvancedSensitiveWords
import io.wdsj.asw.bukkit.AdvancedSensitiveWords.LOGGER
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.Executors

object LoggingUtils {
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd-HH:mm:ss")
    private val loggingThreadPool = Executors.newSingleThreadExecutor(
        ThreadFactoryBuilder()
            .setNameFormat("ASW Logging Thread")
            .setDaemon(true)
            .build()
    )
    @JvmStatic
    fun logViolation(playerName: String, violationReason: String) {
        loggingThreadPool.submit {
            val formattedDate = LocalDateTime.now().format(dateFormatter)
            val logMessage = "[$formattedDate] $playerName $violationReason"
            val logFile = File(AdvancedSensitiveWords.getInstance().dataFolder, "violations.log")
            if (!logFile.exists()) {
                try {
                    logFile.createNewFile()
                } catch (e: IOException) {
                    LOGGER.warning("Failed to create violations.log file: " + e.message)
                    return@submit
                }
            }
            try {
                OutputStreamWriter(FileOutputStream(logFile, true), StandardCharsets.UTF_8).use { writer ->
                    writer.write(logMessage + System.lineSeparator())
                }
            } catch (e: IOException) {
                LOGGER.warning("Failed to write to violations.log file: " + e.message)
            }
        }
    }

    @JvmStatic
    fun purgeLog() {
        val logFile = File(AdvancedSensitiveWords.getInstance().dataFolder, "violations.log")
        if (!logFile.exists()) return
        FileUtil.deleteFile(logFile)
        LOGGER.info("Successfully purged violations")
    }

}
