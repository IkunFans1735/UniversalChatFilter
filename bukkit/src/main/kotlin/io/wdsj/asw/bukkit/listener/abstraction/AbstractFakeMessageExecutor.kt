package io.wdsj.asw.bukkit.listener.abstraction

import org.bukkit.entity.Player
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

abstract class AbstractFakeMessageExecutor {
    protected fun shouldFakeMessage(player: Player): Boolean {
        return FAKE_MESSAGE_NUM.getOrPut(player.uniqueId) { 0 } > 0
    }

    companion object {
        @JvmStatic
        private val FAKE_MESSAGE_NUM = ConcurrentHashMap<UUID, Int>()
        @JvmStatic
        fun selfDecrement(player: Player) {
            val uuid = player.uniqueId
            val currentNum = FAKE_MESSAGE_NUM.getOrPut(uuid) { 0 }
            if (currentNum > 0) {
                FAKE_MESSAGE_NUM[uuid] = currentNum - 1
            }
        }

        @JvmStatic
        fun selfIncrement(player: Player) {
            val uuid = player.uniqueId
            FAKE_MESSAGE_NUM[uuid] = FAKE_MESSAGE_NUM.getOrPut(uuid) { 0 } + 1
        }
    }
}