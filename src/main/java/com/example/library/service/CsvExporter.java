package com.example.library.service;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Writes tabular data as RFC 4180 CSV, which is what spreadsheets expect.
 *
 * <p>Values containing a comma, quote, newline or leading whitespace are quoted, and embedded
 * quotes are doubled.
 */
public final class CsvExporter {

    private CsvExporter() {
    }

    /**
     * Writes a header row followed by the data rows.
     *
     * @param target  file to create or overwrite
     * @param headers column titles
     * @param rows    row values; each row should have as many cells as there are headers
     * @throws IOException when the file cannot be written
     */
    public static void write(Path target, List<String> headers, List<List<String>> rows)
            throws IOException {
        try (Writer writer = Files.newBufferedWriter(target, StandardCharsets.UTF_8)) {
            write(writer, headers, rows);
        }
    }

    /** Writes to an already open writer, which is what the tests use. */
    public static void write(Writer writer, List<String> headers, List<List<String>> rows)
            throws IOException {
        BufferedWriter out = writer instanceof BufferedWriter buffered
                ? buffered
                : new BufferedWriter(writer);
        writeRow(out, headers);
        for (List<String> row : rows) {
            writeRow(out, row);
        }
        out.flush();
    }

    private static void writeRow(BufferedWriter out, List<String> cells) throws IOException {
        for (int i = 0; i < cells.size(); i++) {
            if (i > 0) {
                out.write(',');
            }
            out.write(escape(cells.get(i)));
        }
        // RFC 4180 specifies CRLF line endings.
        out.write("\r\n");
    }

    /** Quotes a single value when it would otherwise confuse a CSV reader. */
    static String escape(String value) {
        if (value == null) {
            return "";
        }
        boolean needsQuotes = value.indexOf(',') >= 0
                || value.indexOf('"') >= 0
                || value.indexOf('\n') >= 0
                || value.indexOf('\r') >= 0
                || value.startsWith(" ")
                || value.endsWith(" ");
        if (!needsQuotes) {
            return value;
        }
        return '"' + value.replace("\"", "\"\"") + '"';
    }
}
