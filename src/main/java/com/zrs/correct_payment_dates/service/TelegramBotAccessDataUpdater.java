package com.zrs.correct_payment_dates.service;

import com.zrs.correct_payment_dates.config.google_sheets.GoogleSheetsHandler;
import com.zrs.correct_payment_dates.entities.GoogleSheetData;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.*;

/**
 * Class {@code TelegramBotAccessDataUpdater} contains service methods to check and update white list - list of users
 * allowed to work with this bot.
 *
 * @author Roman Zaichenko
 * @version 1.0
 * @since 2024-12-15
 */
@Slf4j
@Component
@Getter
@Setter
@RequiredArgsConstructor
public class TelegramBotAccessDataUpdater {
    private Map<Long, String> whiteList = new HashMap<>();

    private final GoogleSheetData accessTableData = new GoogleSheetData.Builder()
            .setFirstColumn("Chat ID")
            .setLastColumn("User name")
            .setSpreadsheetId("1l8UJmzXG0Itn7pgPT1J6xZwrdoCpxmQdqdVjHj_dYWQ")
            .setSpreadsheetListName("Data!")
            .setRange("Data!A1:FG").build();

    private final GoogleSheetsHandler googleSheetsHandler;

    /**
     * Automatically updates white list with fixed delay.
     */
    @Scheduled(fixedDelay = 60000)
    private void updateAccessDataBySchedule() throws GeneralSecurityException, IOException {
        whiteList = checkAndUpdateTelegramBotAccess();
        log.info("White list was updated: {}", whiteList);
    }

    /**
     * Manually updates white list.
     */
    void updateAccessData() throws GeneralSecurityException, IOException {
        whiteList = checkAndUpdateTelegramBotAccess();
    }

    /**
     * Check current users in Access Data spreadsheet table and fill white list.
     *
     * @return map of current user IDs and usernames.
     */
    public HashMap<Long, String> checkAndUpdateTelegramBotAccess() throws GeneralSecurityException, IOException {
        HashMap<Long, String> currentUsersAccess = new HashMap<>();
        int columnRange = googleSheetsHandler.setUsefulValuesRange(accessTableData);
        List<List<Object>> values = googleSheetsHandler.getUsefulValuesFromGoogleSheet(accessTableData);

        if (values == null || values.isEmpty()) {
            System.out.println("No data found.");
        } else {
            values.forEach(row -> {
                if (row.isEmpty()) {
                    System.out.println("No data");
                } else {
                    if (row.size() > columnRange && !row.getLast().toString().isEmpty()) {
                        currentUsersAccess.put(Long.parseLong(String.valueOf(row.getFirst())), row.getLast().toString());
                    } else {
                        System.out.println("Row has insufficient data.");
                    }
                }
            });
        }
        return currentUsersAccess;
    }

    /**
     * Add new user in Access Data spreadsheet table.
     *
     * @param chatId   user chat ID.
     * @param userName username.
     * @return report of the username of added user.
     */
    public String addNewUserToAccessTable(Long chatId, String userName) throws GeneralSecurityException, IOException {
        googleSheetsHandler.setUsefulValuesRange(accessTableData);
        boolean isUserAlreadyInTable = false;
        List<List<Object>> values = googleSheetsHandler.getUsefulValuesFromGoogleSheet(accessTableData);

        if (values == null) {
            values = new ArrayList<>(new ArrayList<>());
            values.add(Arrays.asList(chatId, userName));
        } else if (values.isEmpty()) {
            values.add(Arrays.asList(chatId, userName));
        } else {
            for (List<Object> row : values) {
                if (row.contains(chatId.toString())) {
                    isUserAlreadyInTable = true;
                    break;
                }
            }
            if (!isUserAlreadyInTable) {
                values.add(Arrays.asList(chatId, userName));
            }
        }
        googleSheetsHandler.updateGoogleSheet(values, accessTableData);

        return "Пользователь " + userName + " был добавлен в таблицу доступа.";
    }
}
