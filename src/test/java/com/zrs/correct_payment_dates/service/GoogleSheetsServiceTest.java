package com.zrs.correct_payment_dates.service;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import com.zrs.correct_payment_dates.config.google_sheets.GoogleSheetsHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.*;


class GoogleSheetsServiceTest {
    // Тестируемый сервис
    @Mock
    private ExcelService excelService;

    @Mock
    private GoogleSheetsHandler googleSheetsHandler;

    @InjectMocks
    private GoogleSheetsService googleSheetsService;

    @BeforeEach
    void mockInit() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void whenCheckAndUpdateMainSpreadsheet_givenTestUploadedDocData_shouldGetCorrectReport() throws GeneralSecurityException, IOException {
        String correctReport = "Следующие даты были внесены в таблицу:\n" +
                "Счет: 1095 Дата: 18-Nov-2024\n" +
                "Даты данных счетов уже были проставлены:\n" +
                "Cчет: 1504 Дата: 19-Nov-2024\n" +
                "Cчет: 1519 Дата: 18-Nov-2024\n";

        // Original data
        HashMap<Integer, String> originalData = new HashMap<>();
        originalData.put(1504, "19-Nov-2024");
        originalData.put(1095, "18-Nov-2024");
        originalData.put(1519, "18-Nov-2024");

        // Test data
        HashMap<Integer, String> uploadedDocData = new HashMap<>();
        uploadedDocData.put(1504, "19-Nov-2024");
        uploadedDocData.put(1095, "18-Nov-2024");
        uploadedDocData.put(1519, "18-Nov-2024");

        // Mocked values
        List<List<Object>> mockValues = new ArrayList<>();
        mockValues.add(Arrays.asList("19-Nov-2024", 1504));
        mockValues.add(Arrays.asList("", 1095));
        mockValues.add(Arrays.asList("18-Nov-2024", 1519));

        // Mocks setup
        when(googleSheetsHandler.getUsefulValuesFromGoogleSheet(any())).thenReturn(mockValues);
        when(googleSheetsHandler.setUsefulValuesRange(any())).thenReturn(1);
        when(excelService.extractNumberFromString("1504")).thenReturn(1504);
        when(excelService.extractNumberFromString("1095")).thenReturn(1095);
        when(excelService.extractNumberFromString("1519")).thenReturn(1519);
        doNothing().when(googleSheetsHandler).updateGoogleSheet(anyList(), any());

        // testing method invoke
        String reportMessage = googleSheetsService.checkAndUpdateMainSpreadsheet(uploadedDocData);

        // Data check
        assertEquals(originalData, uploadedDocData);

        // Report check
        assertNotNull(reportMessage);
        assertEquals(reportMessage, correctReport);
    }
}

