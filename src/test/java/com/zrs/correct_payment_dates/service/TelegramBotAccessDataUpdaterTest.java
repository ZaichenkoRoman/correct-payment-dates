package com.zrs.correct_payment_dates.service;

import com.zrs.correct_payment_dates.config.google_sheets.GoogleSheetsHandler;
import com.zrs.correct_payment_dates.entities.GoogleSheetData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class TelegramBotAccessDataUpdaterTest {
    private static GoogleSheetData accessTableData;

    @Mock
    private GoogleSheetsHandler googleSheetsHandler;

    @InjectMocks
    private TelegramBotAccessDataUpdater telegramBotAccessDataUpdater;

    @BeforeEach
    void mockInit() {MockitoAnnotations.openMocks(this);}


    @Test
    void whenCheckAndUpdateTelegramBotAccess_givenMockData_shouldGetResultData() throws GeneralSecurityException, IOException {
        // Mock values
        List<List<Object>> mockData = new ArrayList<>();
        mockData.add(Arrays.asList("608803032", "Dexter Holland"));
        mockData.add(Arrays.asList("832187456", "Dan Marsala"));
        mockData.add(Arrays.asList("815697852", "Mitch Allan"));

        // Original values
        HashMap<Long, String> originalData = new HashMap<>();
        originalData.put(608803032L, "Dexter Holland");
        originalData.put(832187456L, "Dan Marsala");
        originalData.put(815697852L, "Mitch Allan");

        // Result values
        HashMap<Long, String> resultData;

        // Mock setup
        when(googleSheetsHandler.setUsefulValuesRange(any())).thenReturn(1);
        when(googleSheetsHandler.getUsefulValuesFromGoogleSheet(any())).thenReturn(mockData);

        resultData = telegramBotAccessDataUpdater.checkAndUpdateTelegramBotAccess();

        // Data check
        assertEquals(originalData, resultData);

    }
}