package demonck.paper.telegram;

import demonck.paper.dto.Report;
import demonck.paper.utils.MuteManager;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.util.ArrayList;
import java.util.List;

public class TelegramNotifier extends TelegramLongPollingBot {

    private static TelegramNotifier _instance;
    private static final String _BOT_USERNAME = "your_bot_username";
    private static final String _BOT_TOKEN = "6753351717:AAGclvOtOMi8le16Yza6P5VSztMeC7jgOP0";
    private static final String _ADMIN_CHAT_ID = "774562085";
    private static final List<Report> reports = new ArrayList<>();
    private static MuteManager muteManager;

    public static void init(MuteManager muteManager) {
        if (_instance == null) {
            try {
                TelegramNotifier.muteManager = muteManager;
                TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
                _instance = new TelegramNotifier();
                botsApi.registerBot(_instance);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void sendReport(String playerName, String reportedPlayerName, String reportedMessages) {
        if (_instance == null) {
            init(muteManager);
        }

        Report report = new Report(playerName, reportedPlayerName, reportedMessages);
        reports.add(report);

        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(_ADMIN_CHAT_ID);
        sendMessage.setText("Новый репорт от " + playerName + " обвиняемый: " + reportedPlayerName);

        try {
            _instance.execute(sendMessage);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getBotUsername() {
        return _BOT_USERNAME;
    }

    @Override
    public String getBotToken() {
        return _BOT_TOKEN;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            if (messageText.equals("/start")) {
                sendStartMessage(update.getMessage().getChatId().toString());
            } else if (messageText.startsWith("/mute")) {
                handleMuteCommand(update);
            }
        } else if (update.hasCallbackQuery()) {
            String callbackData = update.getCallbackQuery().getData();
            String chatId = update.getCallbackQuery().getMessage().getChatId().toString();
            if (callbackData.equals("view_unprocessed_reports")) {
                sendUnprocessedReports(chatId);
            } else if (callbackData.startsWith("view_report_")) {
                int reportIndex = Integer.parseInt(callbackData.substring("view_report_".length()));
                sendReportDetails(chatId, reportIndex);
            } else if (callbackData.startsWith("choose_mute_time_")) {
                String playerName = callbackData.substring("choose_mute_time_".length());
                sendChooseMuteTime(chatId, playerName);
            } else if (callbackData.startsWith("mute_player_")) {
                String[] tokens = callbackData.split("_");
                String playerName = tokens[2];
                int durationMinutes = Integer.parseInt(tokens[3]);
                muteManager.mutePlayerByName(playerName, durationMinutes);
                int reportIndex = getReportIndexByPlayerName(playerName);
                if (reportIndex != -1) {
                    reports.remove(reportIndex);
                    sendMessage(chatId, "Для игрока " + playerName + " мут был включен на " + durationMinutes + " минут.");
                    sendUnprocessedReports(chatId);
                }
            } else if (callbackData.startsWith("delete_report_")) {
                int reportIndex = Integer.parseInt(callbackData.substring("delete_report_".length()));
                reports.remove(reportIndex);
                sendMessage(chatId, "Репорт был удален.");
                sendUnprocessedReports(chatId);
            }
        }
    }

    private void handleMuteCommand(Update update) {
        String chatId = update.getMessage().getChatId().toString();
        String[] tokens = update.getMessage().getText().split(" ");
        if (tokens.length == 3) {
            String playerName = tokens[1];
            int durationMinutes = Integer.parseInt(tokens[2]);
            muteManager.mutePlayerByName(playerName, durationMinutes);
            sendMessage(chatId, "Для игрока " + playerName + " мут был включен на " + durationMinutes + " минут.");
        } else {
            sendMessage(chatId, "Неверный формат команды. Используйте /mute <имя_игрока> <минуты>");
        }
    }

    private void sendStartMessage(String chatId) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setText("Добро пожаловать! Используйте кнопку ниже, чтобы просмотреть все необработанные репорты.");

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        List<InlineKeyboardButton> row = new ArrayList<>();
        InlineKeyboardButton button = new InlineKeyboardButton();
        button.setText("Просмотр необработанных репортов");
        button.setCallbackData("view_unprocessed_reports");
        row.add(button);
        rows.add(row);
        markup.setKeyboard(rows);
        sendMessage.setReplyMarkup(markup);

        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void sendUnprocessedReports(String chatId) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        if (reports.isEmpty()) {
            sendMessage.setText("Репорты отсуствуют.");
            return;
        }

        sendMessage.setText("Необработанные репорты:");

        for (int i = 0; i < reports.size(); i++) {
            Report report = reports.get(i);
            List<InlineKeyboardButton> row = new ArrayList<>();
            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText(report.getReportedPlayerName());
            button.setCallbackData("view_report_" + i);
            row.add(button);
            rows.add(row);
        }

        markup.setKeyboard(rows);
        sendMessage.setReplyMarkup(markup);

        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void sendReportDetails(String chatId, int reportIndex) {
        Report report = reports.get(reportIndex);

        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setText("Репорт от " + report.getReporterName() + " о " + report.getReportedPlayerName() + ":\n" + report.getReportedMessages());

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        List<InlineKeyboardButton> row = new ArrayList<>();
        InlineKeyboardButton muteButton = new InlineKeyboardButton();
        muteButton.setText("Замутить игрока? " + report.getReportedPlayerName() );
        muteButton.setCallbackData("choose_mute_time_" + report.getReportedPlayerName());
        row.add(muteButton);

        InlineKeyboardButton deleteButton = new InlineKeyboardButton();
        deleteButton.setText("Удалить");
        deleteButton.setCallbackData("delete_report_" + reportIndex);
        row.add(deleteButton);

        rows.add(row);
        markup.setKeyboard(rows);
        sendMessage.setReplyMarkup(markup);

        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }


    private void sendChooseMuteTime(String chatId, String playerName) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setText("Выберите время мута для " + playerName + ":");

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        List<InlineKeyboardButton> row = new ArrayList<>();
        InlineKeyboardButton button30min = new InlineKeyboardButton();
        button30min.setText("30 мин");
        button30min.setCallbackData("mute_player_" + playerName + "_30");
        row.add(button30min);

        InlineKeyboardButton button1hour = new InlineKeyboardButton();
        button1hour.setText("1 час");
        button1hour.setCallbackData("mute_player_" + playerName + "_60");
        row.add(button1hour);

        InlineKeyboardButton button5min = new InlineKeyboardButton();
        button5min.setText("5 мин");
        button5min.setCallbackData("mute_player_" + playerName + "_5");
        row.add(button5min);

        rows.add(row);
        markup.setKeyboard(rows);
        sendMessage.setReplyMarkup(markup);

        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void sendMessage(String chatId, String text) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setText(text);
        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private int getReportIndexByPlayerName(String playerName) {
        for (int i = 0; i < reports.size(); i++) {
            if (reports.get(i).getReportedPlayerName().equals(playerName)) {
                return i;
            }
        }
        return -1;
    }
}