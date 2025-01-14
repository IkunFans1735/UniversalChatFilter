package io.wdsj.asw.bukkit.ai;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.wdsj.asw.bukkit.util.VirtualThreadUtils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Interface for AI processors (Maybe more in the future?)
 */
public interface AIProcessor {
    /**
     * Shared thread pool for AI processors
     */
    ExecutorService THREAD_POOL = VirtualThreadUtils.newVirtualThreadPerTaskExecutorOrProvided(Executors.newCachedThreadPool(new ThreadFactoryBuilder().setNameFormat("ASW AIProcessor Thread-%d").setDaemon(true).build()));
}
