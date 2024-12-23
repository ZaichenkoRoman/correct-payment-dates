package com.zrs.correct_payment_dates.service;

import com.zrs.correct_payment_dates.config.google_sheets.GoogleSheetsHandler;
import com.zrs.correct_payment_dates.entities.GoogleSheetData;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.*;

/**
 * Class {@code GoogleSheetsService} represents main application logic of work with Google Spreadsheet.
 *
 * @author Roman Zaichenko
 * @version 1.0
 * @since 2024-12-13
 */
@Service
@RequiredArgsConstructor
@Getter
public class GoogleSheetsService {
    private final ExcelService excelService;
    private final GoogleSheetsHandler googleSheetsHandler;

    private final GoogleSheetData mainTableData = new GoogleSheetData.Builder()
            .setFirstColumn("Дата оплаты")
            .setLastColumn("№ счета")
            .setSpreadsheetId("17WSi2JVxN-vrXgC42HhR9NC-2kX7uRgrehRCAPSpIdM")
            .setSpreadsheetListName("Заказы!")
            .setRange("Заказы!A1:FG").build();

    /**
     * Check is Google spreadsheet Date column filled, and if not fills it with data from uploadedDocData.
     *
     * @param uploadedDocData map with invoice numbers and dates.
     * @return String report of fields were filled and fields were not.
     */
    public String checkAndUpdateMainSpreadsheet(HashMap<Integer, String> uploadedDocData)
            throws GeneralSecurityException, IOException {
        int columnRange = googleSheetsHandler.setUsefulValuesRange(mainTableData);
        List<List<Object>> values = googleSheetsHandler.getUsefulValuesFromGoogleSheet(mainTableData);
        HashMap<Integer, String> alreadyFilledDates = new HashMap<>();
        HashMap<Integer, String> newFilledDates = new HashMap<>();

        if (values == null || values.isEmpty()) {
            System.out.println("No data found.");
        } else {
            values.forEach(row -> {
                if (row.isEmpty()) {
                    System.out.println("No data");
                } else {
                    if (row.size() > columnRange && !row.getLast().toString().isEmpty()) {
                        int invoiceNumber = excelService.extractNumberFromString(row.getLast().toString());
                        uploadedDocData.forEach((key, value) -> {
                            if (row.getFirst().toString().isEmpty() && key == invoiceNumber) {
                                row.set(0, value);
                                uploadedDocData.replace(invoiceNumber, value);
                                newFilledDates.put(key, value);
                            } else if (key == invoiceNumber && (!row.getFirst().toString().isEmpty())) {
                                alreadyFilledDates.put(key, row.getFirst().toString());
                            }
                        });
                    } else {
                        System.out.println("Row has insufficient data.");
                    }
                }
            });
            googleSheetsHandler.updateGoogleSheet(values, mainTableData);
        }
        return buildProceedReportMessage(alreadyFilledDates, newFilledDates);
    }

    /**
     * Creates String report for Proceed method.
     *
     * @param alreadyFilledDates map with invoice numbers and dates that were filled before use of Proceed method.
     * @param newFilledDates     map with invoice numbers and dates that were filled after use of Proceed method.
     * @return String report of fields were filled and fields were not.
     */
    private String buildProceedReportMessage(HashMap<Integer, String> alreadyFilledDates, HashMap<Integer,
            String> newFilledDates) {
        StringBuilder result = new StringBuilder();
        if (!newFilledDates.isEmpty()) {
            result.append("Следующие даты были внесены в таблицу:\n");
            for (Map.Entry<Integer, String> entry : newFilledDates.entrySet()) {
                Integer key = entry.getKey();
                String value = entry.getValue();
                result.append("Счет: ").append(key).append(" Дата: ").append(value).append("\n");
            }
        } else {
            result.append("Непроставленных дат, по данной выписке, не было.\n");
        }

        if (!alreadyFilledDates.isEmpty()) {
            result.append("Даты данных счетов уже были проставлены:\n");
            for (Map.Entry<Integer, String> entry : alreadyFilledDates.entrySet()) {
                Integer key = entry.getKey();
                String value = entry.getValue();
                result.append("Cчет: ").append(key).append(" Дата: ").append(value).append("\n");
            }
        }
        return result.toString();
    }
}
