package com.zrs.correct_payment_dates.integrated;

import com.zrs.correct_payment_dates.service.TelegramBotAccessDataUpdater;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class IntegratedTelegramBotAccessDataUpdaterTest {
    @Autowired
    private TelegramBotAccessDataUpdater telegramBotAccessDataUpdater;

    @Test
    void whenAddNewUserToAccessTable_givenChatIdAndName_shouldAppearInTable()
            throws GeneralSecurityException, IOException {
        long chatId = 234516754L;
        String userName = "Dexter Holland";
        String correctReport = "Пользователь " + userName + " был добавлен в таблицу доступа.";
        String resultReport;
        HashMap<Long, String> tableData;

        resultReport = telegramBotAccessDataUpdater.addNewUserToAccessTable(chatId, userName);
        tableData = telegramBotAccessDataUpdater.checkAndUpdateTelegramBotAccess();

        assertEquals(correctReport, resultReport);
        assertTrue(tableData.containsKey(chatId));
        assertTrue(tableData.containsValue(userName));
    }
}
