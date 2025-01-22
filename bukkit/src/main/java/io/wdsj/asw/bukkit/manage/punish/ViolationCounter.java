package io.wdsj.asw.bukkit.manage.punish;

import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public enum ViolationCounter {
    INSTANCE;
    private final Map<UUID, Long> violationCountMap = new ConcurrentHashMap<>();

    public long getViolationCount(Player player) {
        return violationCountMap.getOrDefault(player.getUniqueId(), 0L);
    }

    public void incrementViolationCount(Player player, long count) {
        violationCountMap.merge(player.getUniqueId(), count, Long::sum);
    }

    public void incrementViolationCount(Player player) {
        incrementViolationCount(player, 1);
    }

    public void resetViolationCount(Player player) {
        violationCountMap.remove(player.getUniqueId());
    }

    public boolean hasViolation(Player player) {
        return violationCountMap.containsKey(player.getUniqueId()) && violationCountMap.get(player.getUniqueId()) > 0L;
    }

    public void resetAllViolations() {
        violationCountMap.clear();
    }
}