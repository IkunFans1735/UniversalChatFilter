package io.wdsj.asw.bukkit.manage.punish;

import io.wdsj.asw.bukkit.proxy.bungee.BungeeSender;
import io.wdsj.asw.bukkit.proxy.velocity.VelocitySender;
import io.wdsj.asw.bukkit.setting.PluginSettings;
import io.wdsj.asw.bukkit.util.CompatibilityUtils;
import io.wdsj.asw.bukkit.util.SchedulingUtils;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.List;
import java.util.Locale;

import static io.wdsj.asw.bukkit.AdvancedSensitiveWords.LOGGER;
import static io.wdsj.asw.bukkit.AdvancedSensitiveWords.settingsManager;

public class Punishment {
    public static void punish(Player player) {
        List<String> punishList = settingsManager.getProperty(PluginSettings.PUNISHMENT);
        if (punishList.isEmpty()) return;
        for (String punish : punishList) {
            try {
                processSinglePunish(player, punish);
            } catch (IllegalArgumentException e) {
                LOGGER.warning("Invalid punishment method: " + punish);
            }
        }
    }

    public static void processSinglePunish(Player player, String method) throws IllegalArgumentException {
        String[] normalPunish = method.split("\\|");
        PunishmentType punishMethod = PunishmentType.getType(normalPunish[0].toUpperCase(Locale.ROOT));
        if (punishMethod == null) {
            throw new IllegalArgumentException("Invalid punishment method " + normalPunish[0].toUpperCase(Locale.ROOT));
        }
        long violationCount = ViolationCounter.getViolationCount(player);
        if (normalPunish.length > 2 && normalPunish[normalPunish.length - 1].toUpperCase(Locale.ROOT).startsWith("VL") && normalPunish[normalPunish.length - 1].length() > 2) {
            String vlCondition = normalPunish[normalPunish.length - 1].substring(2);
            if (!checkViolationCondition(vlCondition, violationCount)) {
                return;
            }
        }
        switch (punishMethod) {
            case DAMAGE:
                try {
                    double damageAmount = (normalPunish.length >= 2) ? Double.parseDouble(normalPunish[1]) : 1.0D;
                    SchedulingUtils.runSyncAtEntityIfFolia(player, () -> player.damage(damageAmount));
                } catch (NumberFormatException e) {
                    SchedulingUtils.runSyncAtEntityIfFolia(player, () -> player.damage(1.0D));
                }
                break;
            case HOSTILE:
                try {
                    double radius = (normalPunish.length >= 2) ? Double.parseDouble(normalPunish[1]) : 10D;
                    makeHostileTowardsPlayer(player, radius);
                } catch (NumberFormatException e) {
                    makeHostileTowardsPlayer(player, 10D);
                }
                break;
            case COMMAND:
                if (normalPunish.length < 2) throw new IllegalArgumentException("Not enough args");
                String command = normalPunish[1].replace("%player%", player.getName()).replace("%PLAYER%", player.getName());
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
                break;
            case COMMAND_PROXY:
                if (normalPunish.length < 2) throw new IllegalArgumentException("Not enough args");
                String command_proxy = normalPunish[1].replace("%player%", player.getName()).replace("%PLAYER%", player.getName());
                if (settingsManager.getProperty(PluginSettings.HOOK_BUNGEECORD)) {
                    BungeeSender.executeBungeeCommand(player, command_proxy);
                }
                if (settingsManager.getProperty(PluginSettings.HOOK_VELOCITY)) {
                    VelocitySender.executeVelocityCommand(player, command_proxy);
                }
                break;
            case EFFECT:
                if (normalPunish.length < 2) throw new IllegalArgumentException("Not enough args");
                String effect = normalPunish[1];
                //noinspection deprecation
                PotionEffectType potionEffect = CompatibilityUtils.isHigherThan(20) ?
                        Registry.POTION_EFFECT_TYPE.get(NamespacedKey.minecraft(effect.toLowerCase(Locale.ROOT))) :
                        PotionEffectType.getByName(effect.toUpperCase(Locale.ROOT));
                if (potionEffect == null) throw new IllegalArgumentException("Unknown potion effect");
                switch (normalPunish.length) {
                    case 2:
                        SchedulingUtils.runSyncAtEntityIfFolia(player, () -> player.addPotionEffect(new PotionEffect(potionEffect, 10, 0)));
                        break;
                    case 3:
                        int duration_3 = Integer.parseInt(normalPunish[2]);
                        SchedulingUtils.runSyncAtEntityIfFolia(player, () -> player.addPotionEffect(new PotionEffect(potionEffect, duration_3 * 20, 0)));
                        break;
                    case 4:
                    default:
                        int duration_4 = Integer.parseInt(normalPunish[2]);
                        int amplifier = Integer.parseInt(normalPunish[3]);
                        SchedulingUtils.runSyncAtEntityIfFolia(player, () -> player.addPotionEffect(new PotionEffect(potionEffect, duration_4 * 20, amplifier)));
                        break;
                }
                break;
            case SHADOW:
                try {
                    long duration = normalPunish.length >= 2 ? Long.parseLong(normalPunish[1]) : 30L;
                    PlayerShadowController.shadowPlayer(player, System.currentTimeMillis(), duration);
                } catch (NumberFormatException e) {
                    PlayerShadowController.shadowPlayer(player, System.currentTimeMillis(), 30L);
                }
                break;
            default:
                throw new IllegalArgumentException("Invalid punishment method");
        }
    }

    private static void makeHostileTowardsPlayer(Player target, double radius) {
        SchedulingUtils.runSyncAtEntityIfFolia(target, () -> {
            List<Entity> entities = target.getNearbyEntities(radius, radius, radius);
            for (Entity entity : entities) {
                SchedulingUtils.runSyncAtEntityIfFolia(entity, () -> {
                    if (entity instanceof Mob && !entity.hasMetadata("NPC")) {
                        Mob mob = (Mob) entity;
                        mob.setTarget(target);
                    }
                });
            }
        });
    }

    private static boolean checkViolationCondition(String vlCondition, long violationCount) {
        try {
            if (vlCondition.startsWith("=")) {
                long targetCount = Long.parseLong(vlCondition.substring(1));
                return violationCount == targetCount;
            } else if (vlCondition.startsWith(">")) {
                long targetCount = Long.parseLong(vlCondition.substring(1));
                return violationCount > targetCount;
            } else if (vlCondition.startsWith("<")) {
                long targetCount = Long.parseLong(vlCondition.substring(1));
                return violationCount < targetCount;
            } else {
                return true;
            }
        } catch (NumberFormatException e) {
            LOGGER.warning("Invalid violation condition: " + vlCondition);
            return false;
        }
    }
}