package com.example.library.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CsvExporterTest {

    private static String export(List<String> headers, List<List<String>> rows) throws IOException {
        StringWriter writer = new StringWriter();
        CsvExporter.write(writer, headers, rows);
        return writer.toString();
    }

    @Test
    @DisplayName("writes a header row followed by the data, separated by CRLF")
    void writesHeaderAndRows() throws IOException {
        String csv = export(List.of("Title", "Copies"),
                List.of(List.of("Effective Java", "3"), List.of("Clean Code", "2")));

        assertEquals("Title,Copies\r\nEffective Java,3\r\nClean Code,2\r\n", csv);
    }

    @Test
    @DisplayName("writes just the header when there are no rows")
    void writesHeaderOnly() throws IOException {
        assertEquals("Title,Copies\r\n", export(List.of("Title", "Copies"), List.of()));
    }

    @Test
    @DisplayName("leaves an ordinary value unquoted")
    void leavesPlainValuesAlone() {
        assertEquals("plain", CsvExporter.escape("plain"));
        assertEquals("Effective Java", CsvExporter.escape("Effective Java"));
        assertEquals("", CsvExporter.escape(""));
    }

    @Test
    @DisplayName("quotes a value containing the field separator")
    void quotesCommas() {
        assertEquals("\"Bloch, Joshua\"", CsvExporter.escape("Bloch, Joshua"));
    }

    @Test
    @DisplayName("quotes a value whose leading or trailing space would be lost")
    void quotesSignificantWhitespace() {
        assertEquals("\" leading\"", CsvExporter.escape(" leading"));
        assertEquals("\"trailing \"", CsvExporter.escape("trailing "));
    }

    @Test
    @DisplayName("doubles embedded quotes so the value survives a round trip")
    void doublesEmbeddedQuotes() {
        assertEquals("\"He said \"\"stop\"\"\"", CsvExporter.escape("He said \"stop\""));
    }

    @Test
    @DisplayName("quotes a value containing a line break")
    void quotesNewlines() {
        assertEquals("\"first\nsecond\"", CsvExporter.escape("first\nsecond"));
        assertEquals("\"first\rsecond\"", CsvExporter.escape("first\rsecond"));
    }

    @Test
    @DisplayName("writes an absent value as an empty field")
    void writesNullAsEmpty() throws IOException {
        String csv = export(List.of("A", "B"), List.of(java.util.Arrays.asList("value", null)));

        assertEquals("A,B\r\nvalue,\r\n", csv);
    }

    @Test
    @DisplayName("saves to a file as UTF-8, so accents and symbols survive")
    void writesFileAsUtf8(@TempDir Path directory) throws IOException {
        Path target = directory.resolve("export.csv");

        CsvExporter.write(target, List.of("Title", "Price"),
                List.of(List.of("Café Culture", "₹45.99")));

        String written = Files.readString(target, StandardCharsets.UTF_8);
        assertEquals("Title,Price\r\nCafé Culture,₹45.99\r\n", written);
    }
}
