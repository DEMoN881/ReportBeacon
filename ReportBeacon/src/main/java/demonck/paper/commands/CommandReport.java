package demonck.paper.commands;

import demonck.paper.listeners.ChatListener;
import demonck.paper.telegram.TelegramNotifier;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

public class CommandReport implements CommandExecutor, TabCompleter {

    private final ChatListener chatListener;
    private final int maxMessagesToCheck = 20; // Максимальное количество последних сообщений для проверки

    public CommandReport(ChatListener chatListener) {
        this.chatListener = chatListener;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Эту команду может выполнить только игрок.");
            return true;
        }

        Player player = (Player) sender;

        if (args.length < 2) {
            player.sendMessage("Использование: /report <ник> <количество сообщений>");
            return true;
        }

        String reportedPlayerName = args[0];
        int numMessages;

        try {
            numMessages = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            player.sendMessage("Пожалуйста, укажите корректное число сообщений.");
            return true;
        }

        if (numMessages <= 0 || numMessages > maxMessagesToCheck) {
            player.sendMessage("Количество сообщений должно быть от 1 до " + maxMessagesToCheck);
            return true;
        }

        Queue<String> playerMessages = chatListener.getPlayerMessages(reportedPlayerName);

        if (playerMessages.isEmpty()) {
            player.sendMessage("Для игрока " + reportedPlayerName + " нет доступных сообщений.");
            return true;
        }

        // Проверяем последние numMessages сообщений
        StringBuilder reportedMessages = new StringBuilder();
        int count = 0;

        for (String message : playerMessages) {
            if (count >= numMessages) {
                break;
            }
            reportedMessages.append(message).append("\n");
            count++;
        }

        // Отправляем репорт в телеграм и сохраняем его в ArrayList
        TelegramNotifier.sendReport(player.getName(), reportedPlayerName, reportedMessages.toString());

        //player.sendMessage("Отправлены последние " + numMessages + " сообщений игрока " + reportedPlayerName + " в телеграм.");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> playerNames = new ArrayList<>();
            for (Player onlinePlayer : sender.getServer().getOnlinePlayers()) {
                playerNames.add(onlinePlayer.getName());
            }
            return playerNames;
        }
        return null;
    }
}
