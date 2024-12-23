package com.zrs.correct_payment_dates.entities;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

/**
 * Class {@code GoogleSheetData} represents entity objects which contains information to connect with
 * Google spreadsheets. Implements builder pattern.
 *
 * @author Roman Zaichenko
 * @version 1.0
 * @since 2024-12-13
 */
@RequiredArgsConstructor
@Getter @Setter
public class GoogleSheetData {
    private String firstColumn;
    private String lastColumn;
    private String spreadsheetId;
    private String spreadsheetListName;
    private String range;


    private GoogleSheetData(Builder builder) {
        this.firstColumn = builder.dateColumn;
        this.lastColumn = builder.invoiceColumn;
        this.spreadsheetId = builder.spreadsheetId;
        this.spreadsheetListName = builder.spreadsheetListName;
        this.range = builder.range;
    }

    public static class Builder {
        private String dateColumn;
        private String invoiceColumn;
        private String spreadsheetId;
        private String spreadsheetListName;
        private String range;

        public Builder setFirstColumn(String dateColumn) {
            this.dateColumn = dateColumn;
            return this;
        }

        public Builder setLastColumn(String invoiceColumn) {
            this.invoiceColumn = invoiceColumn;
            return this;
        }

        public Builder setSpreadsheetId(String spreadsheetId) {
            this.spreadsheetId = spreadsheetId;
            return this;
        }

        public Builder setSpreadsheetListName(String spreadsheetListName) {
            this.spreadsheetListName = spreadsheetListName;
            this.range = spreadsheetListName + "A1:FG"; // Обновление диапазона
            return this;
        }

        public Builder setRange(String range) {
            this.range = range;
            return this;
        }

        public GoogleSheetData build() {
            return new GoogleSheetData(this);
        }
    }

}

