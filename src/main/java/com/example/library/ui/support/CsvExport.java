package com.example.library.ui.support;

import com.example.library.service.CsvExporter;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.Component;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

/** Wires the table views to {@link CsvExporter} through a save dialog. */
public final class CsvExport {

    private CsvExport() {
    }

    /**
     * Asks where to save, then writes the rows as CSV.
     *
     * @param owner      component the dialogs are centred on
     * @param baseName   suggested file name, without the date or extension
     * @param headers    column titles
     * @param rows       row values
     */
    public static void save(Component owner, String baseName, List<String> headers,
                           List<List<String>> rows) {
        if (rows.isEmpty()) {
            Dialogs.showInfo(owner, "Nothing to export",
                    "There are no rows on screen to export. Clear the search and try again.");
            return;
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Export as CSV");
        chooser.setSelectedFile(new java.io.File(baseName + "-" + LocalDate.now() + ".csv"));
        chooser.setFileFilter(new FileNameExtensionFilter("CSV file", "csv"));
        if (chooser.showSaveDialog(owner) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        Path target = chooser.getSelectedFile().toPath();
        if (!target.getFileName().toString().toLowerCase(java.util.Locale.ROOT).endsWith(".csv")) {
            target = target.resolveSibling(target.getFileName() + ".csv");
        }

        try {
            CsvExporter.write(target, headers, rows);
            Dialogs.showSuccess(owner, "Exported " + Formats.plural(rows.size(), "row", "rows")
                    + " to " + target.getFileName() + ".");
        } catch (IOException e) {
            Dialogs.showError(owner, "The file could not be written.", e);
        }
    }
}
