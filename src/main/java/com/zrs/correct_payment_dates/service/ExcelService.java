package com.zrs.correct_payment_dates.service;

import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class {@code ExcelService} represents main application logic of work with XLSX documents.
 *
 * @author Roman Zaichenko
 * @version 1.0
 * @since 2024-12-13
 */
@Service
@RequiredArgsConstructor
public class ExcelService {
    private static final String DATE_COLUMN = "Дата проводки";
    private static final String INVOICE_COLUMN = "Назначение платежа";
    @Value("${excel.service.book.sheet}")
    private int bookSheet;
    private int dateCell = 0;
    private int invoiceCell = 0;

    /**
     * Parse XLSX document to get invoice numbers and dates.
     *
     * @param localFileAddress local address of received XLSX file.
     * @return Map with invoice numbers and dates.
     */
    public HashMap<Integer, String> parseXLSX(String localFileAddress) throws Exception {
        XSSFWorkbook workbook = getXSSWorkbook(localFileAddress);
        HashMap<Integer, String> invoicesAndDates = new HashMap<>();
        Sheet sheet = workbook.getSheetAt(bookSheet);
        findAndSetInvoiceAndDateCell(sheet);

        for (Row row : sheet) {
            Cell cell = row.getCell(invoiceCell);
            if (cell.getCellType() != CellType.BLANK) {
                String regex = "сч([её]ту)?\\s*(№\\s*)?\\d+";
                String text = cell.getStringCellValue().toLowerCase();

                Pattern pattern = Pattern.compile(regex);
                Matcher matcher = pattern.matcher(text);

                while (matcher.find()) {
                    String result = matcher.group();
                    int invoiceNumber;
                    invoiceNumber = extractNumberFromString(result);
                    invoicesAndDates.put(invoiceNumber, row.getCell(dateCell).toString());
                }
            }
        }

        resetInvoiceAndDateCell();
        return invoicesAndDates;
    }

    /**
     * Extract number from string.
     *
     * @param string string to proceed.
     * @return number.
     */
    public Integer extractNumberFromString(String string) {
        String regex = "\\d+";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(string);
        while (matcher.find()) {
            string = matcher.group();
        }
        return Integer.parseInt(string);
    }

    /**
     * Get XSSWorkbook from received via telegram API file.
     *
     * @param fileLocalAddress local file address.
     * @return XSSFWorkbook.
     * @throws IOException if there is no such file.
     */
    private XSSFWorkbook getXSSWorkbook(String fileLocalAddress) throws IOException {
        FileInputStream fis = new FileInputStream(fileLocalAddress);
        XSSFWorkbook workbook = new XSSFWorkbook(fis);
        fis.close();
        return workbook;
    }

    /**
     * Find indexes of Invoice and Data columns.
     *
     * @param sheet file contains xlsx data.
     */
    private void findAndSetInvoiceAndDateCell(Sheet sheet) throws Exception {
        for (Row row : sheet) {
            row.forEach(cell -> {
                if (cell.getCellType() != CellType.NUMERIC) {
                    if (cell.getStringCellValue().equalsIgnoreCase(DATE_COLUMN)) {
                        dateCell = cell.getColumnIndex();
                    } else if (cell.getStringCellValue().equalsIgnoreCase(INVOICE_COLUMN)) {
                        invoiceCell = cell.getColumnIndex();
                    }
                }
            });
        }
        if (dateCell == 0) {
            throw new Exception("В выписке отсутствует, либо было изменено название поля \"Дата проводки\"");
        } else if (invoiceCell == 0) {
            throw new Exception("В выписке отсутствует, либо было изменено название поля \"Назначение платежа\"");
        }
    }

    /**
     * Reset indexes of Invoice and Data in variables.
     */
    private void resetInvoiceAndDateCell() {
        invoiceCell = 0;
        dateCell = 0;
    }
}
