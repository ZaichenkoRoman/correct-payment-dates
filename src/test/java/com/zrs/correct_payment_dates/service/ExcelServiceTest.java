package com.zrs.correct_payment_dates.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

class ExcelServiceTest {
    private ExcelService excelService;

    @BeforeEach
    void setUp() {
        excelService = new ExcelService();
    }

    @Test
    void whenParseXLSX_givenStringTestFilePath_shouldReturnHashMap() throws Exception {
        String testFilePath = "src/test/resources/СберБизнес_Выписка_за_2024_11_18_2024_11_19.xlsx";
        HashMap<Integer, String> result = excelService.parseXLSX(testFilePath);

        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals(13, result.size());
    }

    @Test
    void whenExtractNumberFromString_givenString_shouldReturnNumber() {
        ArrayList<String> testingSubjectsList = new ArrayList<>();
        testingSubjectsList.add("счету №1588");
        testingSubjectsList.add("счету № 1187");
        testingSubjectsList.add("счету 1316");
        testingSubjectsList.add("счету8361");
        testingSubjectsList.add("счету111");
        testingSubjectsList.add("счету11111");
        testingSubjectsList.add("счёту № 1683");
        testingSubjectsList.add("сч №1588");
        testingSubjectsList.add("счет №1588");

        ArrayList<Integer> expectedResults = new ArrayList<>();
        expectedResults.add(1588);
        expectedResults.add(1187);
        expectedResults.add(1316);
        expectedResults.add(8361);
        expectedResults.add(111);
        expectedResults.add(11111);
        expectedResults.add(1683);
        expectedResults.add(1588);
        expectedResults.add(1588);

        assertAll("Extract number from strings", () -> {
            for (int i = 0; i < testingSubjectsList.size(); i++) {
                String testingString = testingSubjectsList.get(i);
                assertEquals(expectedResults.get(i), excelService.extractNumberFromString(testingString),
                        "Failed for string: " + testingString);
            }
        });
    }

//    @Test
//    void whenGetXSSWorkbook_givenStringTestFilePath_shouldReturnXSSFWorkbook() throws IOException {
//        String testFilePath = "src/test/resources/СберБизнес_Выписка_за_2024_11_18_2024_11_19.xlsx";
//        XSSFWorkbook workbook = ExcelService.getXSSWorkbook(testFilePath);
//
//        assertNotNull(workbook);
//        assertTrue(workbook.getNumberOfSheets() > 0);
//    }
}