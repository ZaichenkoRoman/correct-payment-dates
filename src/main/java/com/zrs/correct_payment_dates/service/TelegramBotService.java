package com.zrs.correct_payment_dates.service;

import com.zrs.correct_payment_dates.config.telegram.BotConfig;
import com.zrs.correct_payment_dates.exception_handling.GlobalExceptionHandler;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Document;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Class {@code TelegramBotService} contains service methods to interact with Telegram API.
 *
 * @author Roman Zaichenko
 * @version 1.0
 * @since 2024-12-13
 */
@Slf4j
@Component
public class TelegramBotService extends TelegramLongPollingBot {
    private static final String FILE_SAVE_FOLDER = "src/main/resources/uploaded_files/";
    private static final String SERVICE_PASSWORD = "3412";
    private final Map<Long, UserState> userStates = new HashMap<>();

    private final BotConfig config;
    private final ExcelService excelService;
    private final GoogleSheetsService googleSheetsService;
    private final GlobalExceptionHandler globalExceptionHandler;
    private final TelegramBotAccessDataUpdater telegramBotAccessDataUpdater;

    List<BotCommand> commandList = new ArrayList<>();

    @Value("${admin.chat.id}")
    private Long adminChatId;

    private enum UserState {
        START,
        WAITING_FOR_SERVICE_PASSWORD,
        ACCESS_GRANTED
    }

    public TelegramBotService(BotConfig botConfig, ExcelService excelService, @Value("${bot.token}") String botToken,
                              GoogleSheetsService googleSheetsService, GlobalExceptionHandler globalExceptionHandler,
                              TelegramBotAccessDataUpdater telegramBotAccessDataUpdater) {
        super(botToken);
        this.config = botConfig;
        this.excelService = excelService;
        this.googleSheetsService = googleSheetsService;
        this.globalExceptionHandler = globalExceptionHandler;
        this.telegramBotAccessDataUpdater = telegramBotAccessDataUpdater;
    }

    /**
     * Send message to Telegram user by it chat ID.
     *
     * @param chatId     user chat ID.
     * @param textToSend message to send.
     */
    public void sendMessage(long chatId, String textToSend) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(textToSend);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Send message of error to Telegram bot admin by its chat ID.
     *
     * @param errorMessage error message to send.
     */
    public void sendErrorMessageToAdmin(String errorMessage) throws TelegramApiException {
        sendMessage(adminChatId, errorMessage);
    }


    /**
     * Check if user upload XLSX file and proceed main bot logic.
     *
     * @param update received update data from Telegram API.
     */
    @SneakyThrows
    @Override
    public void onUpdateReceived(Update update) {
        try {
            long chatId = update.getMessage().getChatId();
            String userName = update.getMessage().getChat().getFirstName() + " " + update.getMessage().getChat()
                    .getLastName();
            String messageText = update.getMessage().getText();
            UserState currentState;

            if (userHasAccess(chatId)) {
                log.info("Пользователь [{}] имеет доступ.", telegramBotAccessDataUpdater.getWhiteList().get(chatId));
                currentState = UserState.ACCESS_GRANTED;
            } else {
                currentState = userStates.getOrDefault(chatId, UserState.START);
            }

            Document document = update.getMessage().getDocument();

            if (document != null && !userHasAccess(chatId)) {
                sendMessage(chatId, "Доступ к функциям бота ограничен. Пройдите, пожалуйста, процедуру " +
                        "идентификации, используя команду /login");
            } else {
                switch (currentState) {
                    case START -> {
                        switch (messageText) {
                            case "/start" -> {
                                startCommandReceived(chatId, update.getMessage().getChat().getFirstName());
                                sendMessage(chatId, "Доступ к функциям бота ограничен. " +
                                        "Пройдите, пожалуйста, процедуру идентификации, используя команду /login");
                            }
                            case "/login" -> {
                                sendMessage(chatId, "Введите, пожалуйста, сервисный пароль:");
                                userStates.put(chatId, UserState.WAITING_FOR_SERVICE_PASSWORD);
                            }
                            default -> sendMessage(chatId, "Доступ к функциям бота ограничен. " +
                                    "Пройдите, пожалуйста, процедуру идентификации, используя команду /login");
                        }
                    }
                    case WAITING_FOR_SERVICE_PASSWORD -> {
                        switch (messageText) {
                            case SERVICE_PASSWORD -> {
                                sendMessage(chatId, "Пароль верный. Добавление нового пользователя в базу.");
                                log.info(telegramBotAccessDataUpdater.addNewUserToAccessTable(chatId, userName));
                                telegramBotAccessDataUpdater.updateAccessData();
                                sendMessage(chatId, """
                                        Пользователь добавлен.
                                        Доступ предоставлен.
                                        Загрузите, пожалуйста, XLSX документ.""");
                            }
                            case "/start" -> {
                                startCommandReceived(chatId, update.getMessage().getChat().getFirstName());
                                sendMessage(chatId, "Доступ к функциям бота ограничен. " +
                                        "Пройдите, пожалуйста, процедуру идентификации, используя команду /login");
                            }
                            case "/login" -> sendMessage(chatId, "Введите, пожалуйста, сервисный пароль:");
                            default -> {
                                sendMessage(chatId, "Введен неверный пароль.");
                                log.info("Пользователь ввел неверный пароль. Пользовательский ввод: {}", messageText);
                            }
                        }
                    }
                    case ACCESS_GRANTED -> {
                        if (document != null) {
                            proceedDocument(chatId, document);
                        } else if (!messageText.isEmpty()) {
                            sendMessage(chatId, "Процедура идентификации пройдена, доступ предоставлен. " +
                                    "Приложите, пожалуйста XLSX файл.");
                        } else {
                            sendMessage(chatId, "Файл XLSX не обнаружен. " +
                                    "Пожалуйста, отправьте соответствующий документ.");
                        }
                    }
                    default -> sendMessage(chatId, "Извините, команда не распознана.");
                }
            }
        } catch (Exception e) {
            globalExceptionHandler.handle(e);
            sendMessage(update.getMessage().getChatId(), """
                    Невозможно обработать данный файл.
                    Бот работает только с Excel таблицами формата XLSX.
                    Если приложенный документ соответствует данному формату - \
                    возможно была изменена форма таблицы выписки и в ней отсутствуют нужные столбцы.""");
            log.error("Error: {}", e.getMessage());
            try {
                sendErrorMessageToAdmin("Ошибка в боте: " + e.getMessage());
            } catch (TelegramApiException telegramApiException) {
                System.err.println("Не удалось отправить сообщение: " + telegramApiException.getMessage());
                log.error("Не удалось отправить сообщение: {}", telegramApiException.getMessage());
            }
        }
    }

    /**
     * Do main logic of proceeding received document.
     *
     * @param chatId   user chat ID.
     * @param document file received from user.
     */
    private void proceedDocument(Long chatId, Document document) throws Exception {
        final String fileId = document.getFileId();
        final String fileName = document.getFileName();
        String localFileAddress;
        String reportMessage;

        if (document.getFileName().endsWith(".xlsx")) {
            System.out.println(fileId);
            System.out.println(fileName);

            localFileAddress = uploadFile(fileName, fileId);
            sendMessage(chatId, "Ваш файл загружен!");
            HashMap<Integer, String> uploadedDocData = excelService.parseXLSX(localFileAddress);
            reportMessage = googleSheetsService.checkAndUpdateMainSpreadsheet(uploadedDocData);
            sendMessage(chatId, reportMessage);
            log.info(reportMessage);
        } else {
            sendMessage(chatId, "Выбран некорректный файл. Выберите xlsx файл для загрузки.");
            log.error("User Error: {}", "Пользователь загрузил не xlsx файл");
        }
    }

    /**
     * Check if user chat ID is in the white list.
     *
     * @param chatId file Name received from update.
     * @return true if user is in white list, has access.
     */
    private boolean userHasAccess(Long chatId) {
        System.out.println(telegramBotAccessDataUpdater.getWhiteList());
        return telegramBotAccessDataUpdater.getWhiteList().containsKey(chatId);
    }

    /**
     * Upload file received from user to Telegram server, and save it locally.
     *
     * @param fileName file Name received from update.
     * @param fileId   file ID received from update.
     * @return String address of locally saved file.
     */
    private String uploadFile(String fileName, String fileId) throws IOException, URISyntaxException {
        String localFileAddress = FILE_SAVE_FOLDER + fileName;
        URI uri = new URI("https://api.telegram.org/bot" + config.getToken() + "/getFile?file_id=" + fileId);
        URL url = uri.toURL();
        System.out.println(url);

        BufferedReader reader = new BufferedReader(new InputStreamReader((url.openStream())));
        String getFileResponse = reader.readLine();

        JSONObject jResult = new JSONObject(getFileResponse);
        JSONObject path = jResult.getJSONObject("result");
        String filePath = path.getString("file_path");
        System.out.println(filePath);

        File localFile = new File(localFileAddress);
        InputStream inputStream = (new URI("https://api.telegram.org/file/bot" + config.getToken()
                + "/" + filePath)).toURL().openStream();

        FileUtils.copyInputStreamToFile(inputStream, localFile);

        reader.close();
        inputStream.close();

        System.out.println("Uploaded!");
        return localFileAddress;
    }

    /**
     * Send greetings to user when he types /start
     *
     * @param chatId user Chat ID.
     * @param name   user Name.
     */
    private void startCommandReceived(long chatId, String name) {
        String answer = "Здравствуйте " + name + "!";
        sendMessage(chatId, answer);
    }

    @Override
    public String getBotUsername() {
        return config.getBotName();
    }

    @Override
    public void onRegister() {
        commandList.add(new BotCommand("/start", "Получить основную информацию о боте"));
        commandList.add(new BotCommand("/login", "Пройти процедуру авторизации для работы с ботом"));

        try {
            execute(new SetMyCommands(commandList, new BotCommandScopeDefault(), null));
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
        super.onRegister();
    }
}
