package com.example.library.ui.support;

import com.example.library.ui.theme.Theme;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

/**
 * Builds label-above-field forms with consistent spacing.
 *
 * <p>Every dialog in the application lays its fields out through this class, which is what keeps
 * the gaps, label styling and helper text identical from one form to the next.
 */
public final class FormBuilder {

    private final JPanel panel = new JPanel(new GridBagLayout());
    private final int columns;

    private int row;
    private int column;

    /**
     * @param columns how many fields sit side by side before wrapping to the next line
     */
    public FormBuilder(int columns) {
        this.columns = Math.max(1, columns);
        panel.setOpaque(false);
    }

    /** Adds a labelled field that occupies one column. */
    public FormBuilder add(String label, JComponent field) {
        return add(label, field, null, 1);
    }

    /** Adds a labelled field with helper text underneath. */
    public FormBuilder add(String label, JComponent field, String helperText) {
        return add(label, field, helperText, 1);
    }

    /** Adds a labelled field spanning {@code span} columns. */
    public FormBuilder add(String label, JComponent field, String helperText, int span) {
        JPanel cell = new JPanel(new java.awt.BorderLayout(0, Theme.SPACE_1));
        cell.setOpaque(false);

        JLabel labelComponent = new JLabel(label);
        labelComponent.setFont(Theme.smallBoldFont());
        labelComponent.setForeground(Theme.textSecondary());
        cell.add(labelComponent, java.awt.BorderLayout.NORTH);
        cell.add(field, java.awt.BorderLayout.CENTER);

        if (helperText != null && !helperText.isBlank()) {
            JLabel helper = new JLabel(helperText);
            helper.setFont(Theme.smallFont());
            helper.setForeground(Theme.textMuted());
            cell.add(helper, java.awt.BorderLayout.SOUTH);
        }

        place(cell, span);
        return this;
    }

    /** Adds a component with no label, e.g. a checkbox or a nested panel. */
    public FormBuilder addBare(JComponent component, int span) {
        place(component, span);
        return this;
    }

    /** Starts a new line even if the current one is not full. */
    public FormBuilder newRow() {
        if (column != 0) {
            column = 0;
            row++;
        }
        return this;
    }

    /** Adds a section heading across the whole width. */
    public FormBuilder section(String title) {
        newRow();
        JLabel heading = new JLabel(title.toUpperCase(java.util.Locale.ROOT));
        heading.setFont(Theme.smallBoldFont());
        heading.setForeground(Theme.textMuted());
        heading.setBorder(Theme.padding(Theme.SPACE_3, 0, 0, 0));
        place(heading, columns);
        return this;
    }

    private void place(JComponent component, int span) {
        int effectiveSpan = Math.min(span, columns);
        if (column + effectiveSpan > columns) {
            column = 0;
            row++;
        }
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = column;
        constraints.gridy = row;
        constraints.gridwidth = effectiveSpan;
        constraints.weightx = effectiveSpan;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.anchor = GridBagConstraints.NORTHWEST;
        constraints.insets = new Insets(0, column == 0 ? 0 : Theme.SPACE_3, Theme.SPACE_3, 0);
        panel.add(component, constraints);

        column += effectiveSpan;
        if (column >= columns) {
            column = 0;
            row++;
        }
    }

    /** The assembled form. */
    public JPanel build() {
        // A trailing filler row absorbs extra height so the fields stay at the top.
        GridBagConstraints filler = new GridBagConstraints();
        filler.gridx = 0;
        filler.gridy = row + 1;
        filler.gridwidth = columns;
        filler.weighty = 1;
        filler.fill = GridBagConstraints.BOTH;
        panel.add(javax.swing.Box.createGlue(), filler);
        return panel;
    }
}
