package demonck.paper.listeners;

import demonck.paper.utils.MuteManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

public class ChatListener implements Listener {
    private final Map<String, Queue<String>> messageHistory = new HashMap<>();
    private final int maxMessages = 20;
    private final MuteManager muteManager;

    public ChatListener(MuteManager muteManager) {
        this.muteManager = muteManager;
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        String playerName = event.getPlayer().getName();

        // Проверяем, есть мут и время истекло ли
        if (muteManager.isPlayerMuted(event.getPlayer().getUniqueId())) {
            muteManager.checkExpiredMutes();
            if (!muteManager.isPlayerMuted(event.getPlayer().getUniqueId())) {
                event.getPlayer().sendMessage("Срок действия мута истёк. Теперь вы можете общаться в чате.");
            } else {
                event.setCancelled(true);
                event.getPlayer().sendMessage("До окончания мута осталось: " + muteManager.getMuteTimeByPlayerName(playerName) + "мс");
            }
            return;
        }

        String message = event.getMessage();

        Queue<String> playerMessages = messageHistory.computeIfAbsent(playerName, k -> new LinkedList<>());

        // Добавляем сообщение в начало очереди (старые сообщения удаляются, если превышен лимит)
        playerMessages.add(message);
        while (playerMessages.size() > maxMessages) {
            playerMessages.poll();
        }
    }

    public Queue<String> getPlayerMessages(String playerName) {
        return messageHistory.getOrDefault(playerName, new LinkedList<>());
    }
}
