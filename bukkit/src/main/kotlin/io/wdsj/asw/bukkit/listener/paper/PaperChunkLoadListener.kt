package io.wdsj.asw.bukkit.listener.paper

import io.wdsj.asw.bukkit.AdvancedSensitiveWords.LOGGER
import io.wdsj.asw.bukkit.annotation.PaperEventHandler
import io.wdsj.asw.bukkit.util.Utils
import org.bukkit.block.Sign
import org.bukkit.block.sign.Side
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.world.ChunkLoadEvent
import java.util.concurrent.CompletableFuture

@PaperEventHandler
class PaperChunkLoadListener : Listener {

    @EventHandler(priority = EventPriority.MONITOR)
    fun onChunkLoad(event: ChunkLoadEvent) {
        val snapshot = event.chunk.chunkSnapshot

        val potentialSignPositions = ArrayList<Triple<Int, Int, Int>>()
        val minY = event.chunk.world.minHeight
        val maxY = event.chunk.world.maxHeight
        CompletableFuture.supplyAsync({
            for (x in 0 until 16) {
                for (z in 0 until 16) {
                    for (y in minY until maxY) {
                        val blockData = snapshot.getBlockData(x, y, z)

                        if (blockData.material.toString().contains("SIGN", true)) {
                            potentialSignPositions.add(Triple(x, y, z))
                        }
                    }
                }
            }
        }, Utils.commonWorker)
            .thenRun {
                if (potentialSignPositions.isNotEmpty()) {
                    LOGGER.warning("Found signs in chunk ${event.chunk.x}, ${event.chunk.z}")
                    LOGGER.warning("Signs found: ${potentialSignPositions.size}")
                    LOGGER.warning("Signs found at: $potentialSignPositions")
                    for (triple in potentialSignPositions) {
                        val block = event.chunk.getBlock(triple.first, triple.second, triple.third)
                        LOGGER.info("Sign at (${triple.first}, ${triple.second}, ${triple.third}), ${block.state}")
                        val state = block.state
                        LOGGER.info(state.toString())
                        if (state is Sign) {
                            val lines = state.getSide(Side.FRONT)

                            LOGGER.info("Sign at (${triple.first}, ${triple.second}, ${triple.third}) contains: $lines")
                        }
                    }
                }
            }
    }
}