package com.zrs.correct_payment_dates.integrated;

import com.zrs.correct_payment_dates.config.google_sheets.GoogleSheetsHandler;
import com.zrs.correct_payment_dates.entities.GoogleSheetData;
import com.zrs.correct_payment_dates.service.ExcelService;
import com.zrs.correct_payment_dates.service.GoogleSheetsService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class IntegratedGoogleSheetsServiceTest {
    @Autowired
    private GoogleSheetsHandler googleSheetsHandler;
    @Autowired
    private GoogleSheetsService googleSheetsService;
    @Autowired
    private ExcelService excelService;

    private static GoogleSheetData googleSheetData;

    @BeforeAll
    static void googleSheetDataObjectInit() {
        googleSheetData = new GoogleSheetData.Builder()
                .setFirstColumn("Дата оплаты")
                .setLastColumn("№ счета")
                .setSpreadsheetId("1FboEkD0eKpVekJfr3bRt5ZECxwS0Yfw4hZ8VDXiA3Ow")
                .setSpreadsheetListName("Заказы!")
                .setRange("Заказы!A1:FG").build();
    }

    @Test
    void whenCheckAndUpdateMainSpreadsheet_givenTestUploadedDocData_googleSheetsDatesFilled_shouldGetCorrectData()
            throws GeneralSecurityException, IOException {
        HashMap<Integer, String> testUploadedDocData = new HashMap<>();
        HashMap<Integer, String> resultMap = new HashMap<>();
        testUploadedDocData.put(1504, "19-Nov-2024");
        testUploadedDocData.put(1095, "18-Nov-2024");
        testUploadedDocData.put(1519, "18-Nov-2024");

        googleSheetsService.checkAndUpdateMainSpreadsheet(testUploadedDocData);
        List<List<Object>> values = googleSheetsHandler.getUsefulValuesFromGoogleSheet(googleSheetData);
        int columnRange = googleSheetsHandler.getValueRangeDifference();

        values.forEach(row -> {
            if (row.size() > columnRange && !row.getLast().toString().isEmpty()) {
                int invoiceNumber = excelService.extractNumberFromString(row.getLast().toString());
                testUploadedDocData.forEach((key, value) -> {
                    if (key == invoiceNumber && value.equals(row.getFirst().toString())) {
                        resultMap.put(key, value);
                    }
                });
            }
        });
        assertEquals(testUploadedDocData, resultMap);
    }

    @Test
    void whenCheckAndUpdateMainSpreadsheet_givenTestUploadedDocData_googleSheetsDatesUnfilled_shouldGetCorrectData()
            throws GeneralSecurityException, IOException {
        HashMap<Integer, String> testUploadedDocData = new HashMap<>();
        HashMap<Integer, String> resultMap = new HashMap<>();
        testUploadedDocData.put(1504, "19-Nov-2024");
        testUploadedDocData.put(1095, "18-Nov-2024");
        testUploadedDocData.put(1519, "18-Nov-2024");

        googleSheetsService.checkAndUpdateMainSpreadsheet(testUploadedDocData);
        List<List<Object>> values = googleSheetsHandler.getUsefulValuesFromGoogleSheet(googleSheetData);
        int columnRange = googleSheetsHandler.getValueRangeDifference();

        values.forEach(row -> {
            if (row.size() > columnRange && !row.getLast().toString().isEmpty()) {
                int invoiceNumber = excelService.extractNumberFromString(row.getLast().toString());
                testUploadedDocData.forEach((key, value) -> {
                    if (key == invoiceNumber && value.equals(row.getFirst().toString())) {
                        resultMap.put(key, value);
                    }
                });
            }
        });
        assertEquals(testUploadedDocData, resultMap);
    }
}