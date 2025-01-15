package io.wdsj.asw.bukkit.command;

import com.github.houbb.heaven.util.util.OsUtil;
import io.wdsj.asw.bukkit.AdvancedSensitiveWords;
import io.wdsj.asw.bukkit.manage.punish.Punishment;
import io.wdsj.asw.bukkit.manage.punish.ViolationCounter;
import io.wdsj.asw.bukkit.permission.PermissionsEnum;
import io.wdsj.asw.bukkit.setting.PluginMessages;
import io.wdsj.asw.bukkit.setting.PluginSettings;
import io.wdsj.asw.bukkit.util.cache.BookCache;
import io.wdsj.asw.bukkit.util.message.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.github.houbb.heaven.util.util.OsUtil.is64;
import static com.github.houbb.heaven.util.util.OsUtil.isUnix;
import static io.wdsj.asw.bukkit.AdvancedSensitiveWords.*;
import static io.wdsj.asw.bukkit.util.TimingUtils.*;
import static io.wdsj.asw.bukkit.util.Utils.getMinecraftVersion;
import static io.wdsj.asw.bukkit.util.Utils.messagesFilteredNum;

public class ConstructCommandExecutor implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (args.length == 1) {
            if (args[0].equalsIgnoreCase("reload") && (sender.hasPermission(PermissionsEnum.RELOAD.getPermission()) || sender instanceof ConsoleCommandSender)) {
                if (!isInitialized) {
                    return true;
                }
                settingsManager.reload();
                File msgFile = new File(getInstance().getDataFolder(), "messages_" + settingsManager.getProperty(PluginSettings.PLUGIN_LANGUAGE) +
                        ".yml");
                if (!msgFile.exists()) {
                    getInstance().saveResource("messages_" + settingsManager.getProperty(PluginSettings.PLUGIN_LANGUAGE) + ".yml", true);
                }
                messagesManager.reload();
                sensitiveWordBs.destroy();
                getInstance().doInitTasks();
                if (settingsManager.getProperty(PluginSettings.BOOK_CACHE_CLEAR_ON_RELOAD) &&
                        settingsManager.getProperty(PluginSettings.BOOK_CACHE)) BookCache.invalidateAll();
                MessageUtils.sendMessage(sender, PluginMessages.MESSAGE_ON_COMMAND_RELOAD);
                return true;
            }
            if (args[0].equalsIgnoreCase("reload")) {
                MessageUtils.sendMessage(sender, PluginMessages.NO_PERMISSION);
                return true;
            }
            if (args[0].equalsIgnoreCase("reloadconfig") && (sender.hasPermission(PermissionsEnum.RELOAD.getPermission()) || sender instanceof ConsoleCommandSender)) {
                settingsManager.reload();
                File msgFile = new File(getInstance().getDataFolder(), "messages_" + settingsManager.getProperty(PluginSettings.PLUGIN_LANGUAGE) +
                        ".yml");
                if (!msgFile.exists()) {
                    getInstance().saveResource("messages_" + settingsManager.getProperty(PluginSettings.PLUGIN_LANGUAGE) + ".yml", true);
                }
                messagesManager.reload();
                MessageUtils.sendMessage(sender, PluginMessages.MESSAGE_ON_COMMAND_RELOAD);
                return true;
            }
            if (args[0].equalsIgnoreCase("reloadconfig")) {
                MessageUtils.sendMessage(sender, PluginMessages.NO_PERMISSION);
                return true;
            }
            if (args[0].equalsIgnoreCase("status") && (sender.hasPermission(PermissionsEnum.STATUS.getPermission()) || sender instanceof ConsoleCommandSender)) {
                MessageUtils.sendMessage(sender, messagesManager.getProperty(PluginMessages.MESSAGE_ON_COMMAND_STATUS).replace("%num%", String.valueOf(messagesFilteredNum.get())).replace("%mode%", isEventMode() ? "Event" : "Packet").replace("%init%", isInitialized ? "true" : "false").replace("%ms%", getProcessAverage() + "ms").replace("%version%", AdvancedSensitiveWords.PLUGIN_VERSION).replace("%mc_version%", getMinecraftVersion()).replace("%platform%", OsUtil.isWindows() ? "Windows" : (OsUtil.isMac() ? "Mac" : isUnix() ? "Linux" : "Unknown")).replace("%bit%", is64() ? "64bit" : "32bit").replace("%java_version%", getJvmVersion()).replace("%java_vendor%", getJvmVendor()));
                return true;
            }
            if (args[0].equalsIgnoreCase("status")) {
                MessageUtils.sendMessage(sender, PluginMessages.NO_PERMISSION);
                return true;
            }
            if (args[0].equalsIgnoreCase("help") && (sender.hasPermission(PermissionsEnum.HELP.getPermission()) || sender instanceof ConsoleCommandSender)) {
                MessageUtils.sendMessage(sender, PluginMessages.MESSAGE_ON_COMMAND_HELP);
                return true;
            }
            if (args[0].equalsIgnoreCase("help")) {
                MessageUtils.sendMessage(sender, PluginMessages.NO_PERMISSION);
                return true;
            }
        }
        if (args.length >= 1) {
            if (args[0].equalsIgnoreCase("test") && (sender.hasPermission(PermissionsEnum.TEST.getPermission()) || sender instanceof ConsoleCommandSender)) {
                if (args.length >= 2) {
                    if (isInitialized) {
                        StringBuilder sb = new StringBuilder();
                        for (int i = 1; i <= args.length - 1; i++) {
                            sb.append(args[i]).append(" ");
                        }
                        String testArgs = sb.toString();
                        List<String> censoredWordList = sensitiveWordBs.findAll(testArgs);
                        if (!censoredWordList.isEmpty()) {
                            MessageUtils.sendMessage(sender, messagesManager.getProperty(PluginMessages.MESSAGE_ON_COMMAND_TEST).replace("%original_msg%", testArgs).replace("%processed_msg%", sensitiveWordBs.replace(testArgs)).replace("%censored_list%", censoredWordList.toString()));
                        } else {
                            MessageUtils.sendMessage(sender, PluginMessages.MESSAGE_ON_COMMAND_TEST_PASS);
                        }
                    } else {
                        MessageUtils.sendMessage(sender, PluginMessages.MESSAGE_ON_COMMAND_TEST_NOT_INIT);
                    }
                } else {
                    MessageUtils.sendMessage(sender, PluginMessages.NOT_ENOUGH_ARGS);
                }
                return true;
            }
            if (args[0].equalsIgnoreCase("test") && args.length == 1) {
                MessageUtils.sendMessage(sender, PluginMessages.NO_PERMISSION);
                return true;
            }
            if (args[0].equalsIgnoreCase("add") && (sender.hasPermission(PermissionsEnum.ADD.getPermission()) || sender instanceof ConsoleCommandSender)) {
                if (args.length > 1) {
                    List<String> words = new ArrayList<>(Arrays.asList(args).subList(1, args.length));
                    sensitiveWordBs.addWord(words);
                    MessageUtils.sendMessage(sender, PluginMessages.MESSAGE_ON_COMMAND_ADD_SUCCESS);
                } else {
                    MessageUtils.sendMessage(sender, PluginMessages.NOT_ENOUGH_ARGS);
                }
                return true;
            }
            if (args[0].equalsIgnoreCase("add")) {
                MessageUtils.sendMessage(sender, PluginMessages.NO_PERMISSION);
                return true;
            }
            if (args[0].equalsIgnoreCase("remove") && (sender.hasPermission(PermissionsEnum.REMOVE.getPermission()) || sender instanceof ConsoleCommandSender)) {
                if (args.length > 1) {
                    List<String> words = new ArrayList<>(Arrays.asList(args).subList(1, args.length));
                    sensitiveWordBs.removeWord(words);
                    MessageUtils.sendMessage(sender, PluginMessages.MESSAGE_ON_COMMAND_REMOVE_SUCCESS);
                } else {
                    MessageUtils.sendMessage(sender, PluginMessages.NOT_ENOUGH_ARGS);
                }
                return true;
            }
            if (args[0].equalsIgnoreCase("addallow") && (sender.hasPermission(PermissionsEnum.ADD.getPermission()) || sender instanceof ConsoleCommandSender)) {
                if (args.length > 1) {
                    List<String> words = new ArrayList<>(Arrays.asList(args).subList(1, args.length));
                    sensitiveWordBs.addWordAllow(words);
                    MessageUtils.sendMessage(sender, PluginMessages.MESSAGE_ON_COMMAND_ADD_SUCCESS);
                } else {
                    MessageUtils.sendMessage(sender, PluginMessages.NOT_ENOUGH_ARGS);
                }
                return true;
            }
            if (args[0].equalsIgnoreCase("addallow")) {
                MessageUtils.sendMessage(sender, PluginMessages.NO_PERMISSION);
                return true;
            }
            if (args[0].equalsIgnoreCase("removeallow") && (sender.hasPermission(PermissionsEnum.REMOVE.getPermission()) || sender instanceof ConsoleCommandSender)) {
                if (args.length > 1) {
                    List<String> words = new ArrayList<>(Arrays.asList(args).subList(1, args.length));
                    sensitiveWordBs.removeWordAllow(words);
                    MessageUtils.sendMessage(sender, PluginMessages.MESSAGE_ON_COMMAND_REMOVE_SUCCESS);
                } else {
                    MessageUtils.sendMessage(sender, PluginMessages.NOT_ENOUGH_ARGS);
                }
                return true;
            }
            if (args[0].equalsIgnoreCase("removeallow")) {
                MessageUtils.sendMessage(sender, PluginMessages.NO_PERMISSION);
                return true;
            }
            if (args[0].equalsIgnoreCase("remove")) {
                MessageUtils.sendMessage(sender, PluginMessages.NO_PERMISSION);
                return true;
            }
            if (args[0].equalsIgnoreCase("info") && (sender.hasPermission(PermissionsEnum.INFO.getPermission()) || sender instanceof ConsoleCommandSender)) {
                if (args.length > 1) {
                    String playerName = args[1];
                    Player target = Bukkit.getPlayer(playerName);
                    if (target == null) {
                        MessageUtils.sendMessage(sender, PluginMessages.PLAYER_NOT_FOUND);
                        return true;
                    }
                    String violations = String.valueOf(ViolationCounter.getViolationCount(target));
                    MessageUtils.sendMessage(sender, messagesManager.getProperty(PluginMessages.MESSAGE_ON_PLAYER_INFO).replace("%player%", playerName).replace("%violation%", violations));
                } else {
                    MessageUtils.sendMessage(sender, PluginMessages.NOT_ENOUGH_ARGS);
                }
                return true;
            }
            if (args[0].equalsIgnoreCase("info")) {
                MessageUtils.sendMessage(sender, PluginMessages.NO_PERMISSION);
                return true;
            }
            if (args[0].equalsIgnoreCase("reset") && (sender.hasPermission(PermissionsEnum.RESET.getPermission()) || sender instanceof ConsoleCommandSender)) {
                if (args.length > 1) {
                    String playerName = args[1];
                    Player target = Bukkit.getPlayer(playerName);
                    if (target == null) {
                        MessageUtils.sendMessage(sender, PluginMessages.PLAYER_NOT_FOUND);
                        return true;
                    }
                    ViolationCounter.resetViolationCount(target);
                    MessageUtils.sendMessage(sender, messagesManager.getProperty(PluginMessages.MESSAGE_ON_COMMAND_RESET).replace("%player%", playerName));
                } else {
                    MessageUtils.sendMessage(sender, PluginMessages.NOT_ENOUGH_ARGS);
                }
                return true;
            }
            if (args[0].equalsIgnoreCase("reset")) {
                MessageUtils.sendMessage(sender, PluginMessages.NO_PERMISSION);
                return true;
            }
            if (args[0].equalsIgnoreCase("punish") && (sender.hasPermission(PermissionsEnum.PUNISH.getPermission()) || sender instanceof ConsoleCommandSender)) {
                if (args.length >= 2) {
                    String playerName = args[1];
                    Player player = Bukkit.getPlayer(playerName);
                    if (player != null) {
                        if (args.length >= 3) {
                            StringBuilder method = new StringBuilder(args[2]);
                            if (args.length >= 4) {
                                for (int i = 3; i <= args.length - 1; i++) {
                                    method.append(" ").append(args[i]);
                                }
                            }
                            try {
                                Punishment.processSinglePunish(player, method.toString().trim());
                                MessageUtils.sendMessage(sender, messagesManager.getProperty(PluginMessages.MESSAGE_ON_COMMAND_PUNISH_SUCCESS).replace("%player%", player.getName()));
                            } catch (IllegalArgumentException e) {
                                MessageUtils.sendMessage(sender, PluginMessages.MESSAGE_ON_COMMAND_PUNISH_PARSE_ERROR);
                            }
                        } else {
                            Punishment.punish(player);
                            MessageUtils.sendMessage(sender, messagesManager.getProperty(PluginMessages.MESSAGE_ON_COMMAND_PUNISH_SUCCESS).replace("%player%", player.getName()));
                        }
                    } else {
                        MessageUtils.sendMessage(sender, PluginMessages.PLAYER_NOT_FOUND);
                    }
                } else {
                    MessageUtils.sendMessage(sender, PluginMessages.NOT_ENOUGH_ARGS);
                }
                return true;
            }
            if (args[0].equalsIgnoreCase("punish")) {
                MessageUtils.sendMessage(sender, PluginMessages.NO_PERMISSION);
                return true;
            }
        }
        MessageUtils.sendMessage(sender, PluginMessages.UNKNOWN_COMMAND);
        return true;
    }

}
