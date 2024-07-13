package demonck.paper;

import demonck.paper.commands.CommandReport;
import demonck.paper.listeners.ChatListener;
import demonck.paper.telegram.TelegramNotifier;
import demonck.paper.utils.MuteManager;
import org.bukkit.plugin.java.JavaPlugin;


public final class reportBeacon extends JavaPlugin {

    private MuteManager _muteManager;

    @Override
    public void onEnable() {

        _muteManager = new MuteManager();

        ChatListener chatListener = new ChatListener(_muteManager);
        getServer().getPluginManager().registerEvents(chatListener, this);
        getCommand("report").setExecutor(new CommandReport(chatListener));

        // Инициализация Telegram бота
        TelegramNotifier.init(_muteManager);
    }

    @Override
    public void onDisable() {

    }
}
