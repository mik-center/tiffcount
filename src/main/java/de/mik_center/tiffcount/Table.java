// Licensed under the Apache License, Version 2.0
// See http://www.apache.org/licenses/LICENSE-2.0

package de.mik_center.tiffcount;

import static java.nio.charset.StandardCharsets.*;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.Map.*;

/**
 * A two-dimensional array of {@code String} with named columns and rows
 * referenced by index. It offers the convenience of exporting to CSV files.
 */
public class Table {
    private Map<String, ArrayList<String>> tableColumns = new LinkedHashMap<>();

    /**
     * &#x1F511; Adds columns to the table. Only new columns are added.
     * 
     * @param columnName names of the columns to be added
     */
    public void add(String... columnName) {
        for (String column : columnName) {
            tableColumns.computeIfAbsent(column, caption -> new ArrayList<>());
        }
    }

    /**
     * &#x1F511; Enters a value into a cell.
     * 
     * @param row        line number
     * @param columnName column heading
     * @param value      value
     */
    public void set(int row, String columnName, String value) {
        List<String> column = tableColumns.get(columnName);
        assert column != null : "'columnName' is unknown, was: " + columnName;
        while (row > column.size()) column.add("");
        if (row == column.size()) column.add(value);
            else column.set(row, value);
    }

    /**
     * Enters a value into a cell. This is a convenient way to enter long values.
     * 
     * @param row        line number
     * @param columnName column heading
     * @param value      value
     */
    public void set(int row, String columnName, long value) {
        set(row, columnName, Long.toString(value));
    }

    /**
     * Writes a comma-separated value file. The values ​​are actually separated by
     * semicolons, not commas. Values ​​containing a semicolon or quotation mark are
     * enclosed in quotation marks, and any quotation marks within the semicolon are
     * doubled. Control characters are replaced by their equivalents ({@code \r\n}) or a
     * hexadecimal notation ({@code \xA0}). The file is saved as UTF-8 with BOM.
     * 
     * @param fileToWrite path for the file to be created. An existing file will be
     *                    overwritten.
     */
    public void toCSV(Path fileToWrite) throws IOException {
        try (PrintWriter out = new PrintWriter(Files.newBufferedWriter(fileToWrite, UTF_8))) {
            out.write('\uFEFF'); // byte-order mark

            int numberOfDataRows = 0;
            boolean colon = false;
            Set<Entry<String, ArrayList<String>>> iterableColumns = tableColumns.entrySet();
            for (Entry<String, ArrayList<String>> column : iterableColumns) {
                if (colon) out.print(';'); else colon = true;
                out.print(quoteNonDigitContent(column.getKey()));
                int columnLength = column.getValue().size();
                if (columnLength > numberOfDataRows) numberOfDataRows = columnLength;
            }
            out.println();

            for (int row = 0; row < numberOfDataRows; row++) {
                colon = false;
                for (Entry<String, ArrayList<String>> column : iterableColumns) {
                    if (colon) out.print(';'); else colon = true;
                    ArrayList<String> columnRows = column.getValue();
                    if (columnRows.size() > row) {
                        String cellData = columnRows.get(row);
                        out.print(cellData != null ? quoteNonDigitContent(cellData) : "");
                    }
                }
                out.println();
            }
        }
    }

    static String quoteNonDigitContent(String cellData) {
        if (cellData.matches("-?\\d*(?:\\.\\d+)?")) return cellData;

        boolean quoted = false;
        StringBuilder safe = new StringBuilder();
        for (char c : cellData.toCharArray()) {
            switch (c) {
                case '\0': safe.append("\\0"); break;
                case '\n': safe.append("\\n"); break;
                case '\r': safe.append("\\r"); break;
                case '\t': safe.append("\\t"); break;
                case '"':
                    safe.append('"');
                // fall through
                case ';':
                    if(!quoted) {
                        safe.insert(0, '"');
                        quoted = true;
                    }
                // fall through
                default:
                    if (c < 32 || c > 126 && c < 161)
                        safe.append(String.format("\\%02X", (int)c));
                        else safe.append(c);
                break;
            }
        }
        if(quoted) safe.append('"');
        return safe.toString();
    }
}
