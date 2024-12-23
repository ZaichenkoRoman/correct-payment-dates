package com.zrs.correct_payment_dates.config.google_sheets;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.UpdateValuesResponse;
import com.google.api.services.sheets.v4.model.ValueRange;
import com.zrs.correct_payment_dates.entities.GoogleSheetData;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Class {@code GoogleSheetsHandler} contains service methods to interact with Google Spreadsheet API.
 *
 * @author Roman Zaichenko
 * @version 1.0
 * @since 2024-12-13
 */
@Component
@RequiredArgsConstructor
@Getter
@Setter
@Slf4j
public class GoogleSheetsHandler {
    private static final String APPLICATION_NAME = "Correct Payment Dates";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private int valueRangeDifference = 0;
    private String usefulValuesRange;

    /**
     * Global instance of the scopes required by this quickstart.
     * If modifying these scopes, delete your previously saved tokens/ folder.
     */
    private static final List<String> SCOPES =
            Collections.singletonList(SheetsScopes.SPREADSHEETS);
    private static final String SERVICE_ACCOUNT_FILE_PATH = "/service-account-key.json";

    /**
     * Gets only useful values from defined Google sheet by using spreadsheetId, and usefulValuesRange as range.
     *
     * @param googleSheetData object, contains info table address, list name and useful columns.
     * @return List of Lists with data from determined in googleSheetData columns.
     */
    public List<List<Object>> getUsefulValuesFromGoogleSheet(GoogleSheetData googleSheetData) throws IOException, GeneralSecurityException {
        // Build a new authorized API client service.
        Sheets service = getSheetsService();
        ValueRange response = service.spreadsheets().values()
                .get(googleSheetData.getSpreadsheetId(), usefulValuesRange)
                .execute();
        return response.getValues();
    }

    /**
     * Update data in Google spreadsheet.
     *
     * @param values          List of Lists of Objects to save in Google spreadsheet
     * @param googleSheetData object, contains info table address, list name and useful columns.
     */
    public void updateGoogleSheet(List<List<Object>> values, GoogleSheetData googleSheetData) throws IOException, GeneralSecurityException {
        // Get Google Sheets API authorization
        Sheets service = getSheetsService();

        // Request body
        ValueRange body = new ValueRange().setValues(values);

        //  Update request
        UpdateValuesResponse result = service.spreadsheets().values()
                .update(googleSheetData.getSpreadsheetId(), usefulValuesRange, body)
                .setValueInputOption("RAW")
                .execute();
    }

    /**
     * Set range of useful data for String usefulValuesRange.
     *
     * @param googleSheetData object, contains info table address, list name and useful columns.
     * @return Int difference between first and last column in useful range
     */
    public int setUsefulValuesRange(GoogleSheetData googleSheetData) throws GeneralSecurityException, IOException {
        ArrayList<Integer> valuesRange = checkColumnNamesAndGetValuesRange(getAllValuesFromGoogleSheet(googleSheetData),
                googleSheetData);
        String dateColumn = convertToColumnName(valuesRange.get(0));
        String invoiceColumn = convertToColumnName(valuesRange.get(1));

        usefulValuesRange = googleSheetData.getSpreadsheetListName() + dateColumn + "2:" + invoiceColumn;
        valueRangeDifference = valuesRange.get(1) - valuesRange.get(0);

        return valuesRange.get(1) - valuesRange.get(0);
    }

    /**
     * Creates an authorized Credential object.
     *
     * @param HTTP_TRANSPORT The network HTTP Transport.
     * @return An authorized Credential object.
     * @throws IOException If the credentials.json file cannot be found.
     */
    private static Credential getServiceAccountCredentials(final NetHttpTransport HTTP_TRANSPORT) throws IOException {
        InputStream in = GoogleSheetsHandler.class.getResourceAsStream(SERVICE_ACCOUNT_FILE_PATH);
        if (in == null) {
            throw new FileNotFoundException("Resource not found: SERVICE_ACCOUNT_FILE_PATH");
        }
        return GoogleCredential.fromStream(in, HTTP_TRANSPORT, JSON_FACTORY)
                .createScoped(SCOPES);
    }

    /**
     * Gets all values from defined Google sheet by using spreadsheetId.
     *
     * @param googleSheetData object, contains info table address, list name and useful columns.
     * @return List of Lists of with data from range which specified in static class variable range.
     */
    private static List<List<Object>> getAllValuesFromGoogleSheet(GoogleSheetData googleSheetData) throws IOException, GeneralSecurityException {
        // Build a new authorized API client service.
        Sheets service = getSheetsService();
        ValueRange response = service.spreadsheets().values()
                .get(googleSheetData.getSpreadsheetId(), googleSheetData.getRange())
                .execute();
        return response.getValues();
    }

    /**
     * Check and find correct columns according to DATE_COLUMN and INVOICE_COLUMN
     *
     * @param values          list of data from the Google spreadsheet
     * @param googleSheetData object, contains info table address, list name and useful columns.
     * @return list where first element is number of column Date and second of column Invoice
     */
    private ArrayList<Integer> checkColumnNamesAndGetValuesRange(List<List<Object>> values,
                                                                 GoogleSheetData googleSheetData) {
        ArrayList<Integer> result = new ArrayList<>();
        int dateColumnNumber = 0;
        int invoiceColumnNumber = 0;

        for (int i = 0; i < values.getFirst().size(); i++) {
            if (values.getFirst().get(i).toString().equals(googleSheetData.getFirstColumn())) {
                dateColumnNumber = i + 1;
            } else if (values.getFirst().get(i).toString().equals(googleSheetData.getLastColumn())) {
                invoiceColumnNumber = i + 1;
            }
        }
        result.add(dateColumnNumber);
        result.add(invoiceColumnNumber);

        return result;
    }

    /**
     * Get sheet service
     *
     * @return Sheet object
     */
    private static Sheets getSheetsService() throws GeneralSecurityException, IOException {
        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        return new Sheets.Builder(HTTP_TRANSPORT, JSON_FACTORY, getServiceAccountCredentials(HTTP_TRANSPORT))
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

    /**
     * Get letter definition of Google spreadsheet column by column number
     *
     * @param columnNumber number of processed column
     * @return letter definition of column number
     */
    private static String convertToColumnName(int columnNumber) {
        StringBuilder columnName = new StringBuilder();

        while (columnNumber > 0) {
            columnNumber--;
            columnName.insert(0, (char) ('A' + columnNumber % 26));
            columnNumber /= 26;
        }
        return columnName.toString();
    }
}
