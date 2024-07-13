package demonck.paper.utils;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;

public class MuteManager {
    private final Set<UUID> mutedPlayers = new HashSet<>();
    private final Map<UUID, Long> mutedUntil = new HashMap<>(); // Для хранения времени размута

    public void mutePlayer(UUID playerUUID) {
        mutedPlayers.add(playerUUID);
    }

    public void unMutePlayer(UUID playerUUID) {
        mutedPlayers.remove(playerUUID);
        mutedUntil.remove(playerUUID);
    }

    public boolean isPlayerMuted(UUID playerUUID) {
        return mutedPlayers.contains(playerUUID);
    }

    public void mutePlayerByName(String playerName, long durationMinutes) {
        Player player = Bukkit.getPlayerExact(playerName);
        if (player != null) {
            mutePlayer(player.getUniqueId());
            if (durationMinutes > 0) {
                mutedUntil.put(player.getUniqueId(), System.currentTimeMillis() + durationMinutes * 60 * 1000);
            }
        }
    }

    public long getMuteTime(UUID playerUUID) {
        Long endTime = mutedUntil.get(playerUUID);
        if (endTime != null) {
            return endTime - System.currentTimeMillis();
        } else {
            return -1;
        }
    }

    public long getMuteTimeByPlayerName(String playerName) {
        Player player = Bukkit.getPlayer(playerName);
        if (player != null) {
            UUID playerUUID = player.getUniqueId();
            return getMuteTime(playerUUID);
        } else {
            return -1;
        }
    }


    public void checkExpiredMutes() {
        long currentTime = System.currentTimeMillis();
        Set<UUID> keysToRemove = new HashSet<>();
        for (Map.Entry<UUID, Long> entry : mutedUntil.entrySet()) {
            if (entry.getValue() <= currentTime) {
                keysToRemove.add(entry.getKey());
            }
        }
        for (UUID uuid : keysToRemove) {
            unMutePlayer(uuid);
        }
    }
}
