package io.wdsj.asw.bukkit.listener.abstraction

import org.bukkit.entity.Player
import java.util.concurrent.ConcurrentHashMap

abstract class AbstractFakeMessageExecutor {
    protected fun shouldFakeMessage(player: Player): Boolean {
        return FAKE_MESSAGE_NUM.getOrPut(player) { 0 } > 0
    }

    companion object {
        @JvmStatic
        private val FAKE_MESSAGE_NUM = ConcurrentHashMap<Player, Int>()
        @JvmStatic
        fun selfDecrement(player: Player) {
            val currentNum = FAKE_MESSAGE_NUM.getOrPut(player) { 0 }
            if (currentNum > 0) {
                FAKE_MESSAGE_NUM[player] = currentNum - 1
            }
        }

        @JvmStatic
        fun selfIncrement(player: Player) {
            FAKE_MESSAGE_NUM[player] = FAKE_MESSAGE_NUM.getOrPut(player) { 0 } + 1
        }
    }
}