package io.wdsj.asw.bukkit.manage.notice;

import io.wdsj.asw.bukkit.manage.punish.ViolationCounter;
import io.wdsj.asw.bukkit.permission.PermissionsEnum;
import io.wdsj.asw.bukkit.permission.cache.CachingPermTool;
import io.wdsj.asw.bukkit.setting.PluginMessages;
import io.wdsj.asw.bukkit.type.ModuleType;
import io.wdsj.asw.bukkit.util.message.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.List;

public class Notifier {
    /**
     * Notice the operators
     * @param violatedPlayer the player who violated the rules
     * @param moduleType the detection module type
     * @param originalMessage original message sent by the player
     * @param censoredList censored list
     */
    public static void notice(Player violatedPlayer, ModuleType moduleType, String originalMessage, List<String> censoredList) {
        Collection<? extends Player> players = Bukkit.getOnlinePlayers();
        String message = MessageUtils.retrieveMessage(PluginMessages.ADMIN_REMINDER).replace("%player%", violatedPlayer.getName()).replace("%type%", moduleType.toString()).replace("%message%", ChatColor.stripColor(originalMessage)).replace("%censored_list%", censoredList.toString()).replace("%violation%", String.valueOf(ViolationCounter.INSTANCE.getViolationCount(violatedPlayer)));
        for (Player player : players) {
            if (CachingPermTool.hasPermission(PermissionsEnum.NOTICE, player)) {
                MessageUtils.sendMessage(player, message);
            }
        }
    }

    public static void normalNotice(String message) {
        Collection<? extends Player> players = Bukkit.getOnlinePlayers();
        for (Player player : players) {
            if (CachingPermTool.hasPermission(PermissionsEnum.NOTICE, player)) {
                MessageUtils.sendMessage(player, message);
            }
        }
    }

    /**
     * Notice Operator method used by the proxy receivers
     * @param violatedPlayer the player who violated the rules, with server name
     * @param eventType the type
     * @param originalMessage the original message sent by the player
     * @param censoredList censored list
     */
    public static void noticeFromProxy(String violatedPlayer, String serverName, String eventType, String violationCount, String originalMessage, String censoredList) {
        Collection<? extends Player> players = Bukkit.getOnlinePlayers();
        String message = MessageUtils.retrieveMessage(PluginMessages.ADMIN_REMINDER_PROXY).replace("%player%", violatedPlayer).replace("%type%", eventType).replace("%message%", ChatColor.stripColor(originalMessage)).replace("%censored_list%", censoredList).replace("%server_name%", serverName).replace("%violation%", violationCount);
        for (Player player : players) {
            if (CachingPermTool.hasPermission(PermissionsEnum.NOTICE, player)) {
                player.sendMessage(message);
            }
        }
    }
}
