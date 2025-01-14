package io.wdsj.asw.bukkit.util

import com.github.Anon8281.universalScheduler.UniversalScheduler
import com.github.Anon8281.universalScheduler.scheduling.tasks.MyScheduledTask
import io.wdsj.asw.bukkit.AdvancedSensitiveWords
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Entity
import org.bukkit.event.Event
import java.util.concurrent.Callable


object SchedulingUtils {
    private val isFolia = UniversalScheduler.isFolia

    @JvmStatic
    fun runSyncIfFolia(runnable: Runnable) {
        if (isFolia) {
            AdvancedSensitiveWords.getScheduler().runTask(runnable)
        } else {
            runnable.run()
        }
    }

    @JvmStatic
    fun runSyncAtEntityIfFolia(entity: Entity, runnable: Runnable) {
        if (isFolia) {
            AdvancedSensitiveWords.getScheduler().runTask(entity, runnable)
        } else {
            runnable.run()
        }
    }

    @JvmStatic
    fun runSyncAtLocationIfFolia(location: Location, runnable: Runnable) {
        if (isFolia) {
            AdvancedSensitiveWords.getScheduler().runTask(location, runnable)
        } else {
            runnable.run()
        }
    }

    @JvmStatic
    fun runSyncIfEventAsync(event: Event, runnable: Runnable) {
        if (event.isAsynchronous) {
            AdvancedSensitiveWords.getScheduler().runTask(runnable)
        } else {
            runnable.run()
        }
    }

    @JvmStatic
    fun runSyncIfNotOnMainThread(runnable: Runnable) {
        if (Bukkit.isPrimaryThread()) {
            runnable.run()
        } else {
            AdvancedSensitiveWords.getScheduler().runTask(runnable)
        }
    }

    @JvmStatic
    fun cancelTaskSafely(task: MyScheduledTask?) {
        task?.cancel()
    }

    @JvmStatic
    fun <T> callSyncMethod(callable: Callable<T>): T {
        return AdvancedSensitiveWords.getScheduler().callSyncMethod(callable).get()
    }
}
