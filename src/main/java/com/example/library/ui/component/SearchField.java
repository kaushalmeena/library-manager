package com.example.library.ui.component;

import com.example.library.ui.theme.Theme;
import com.example.library.ui.theme.VectorIcon;

import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.Dimension;
import java.util.function.Consumer;

/**
 * A search box that reports every keystroke.
 *
 * <p>FlatLaf draws the leading icon and the clear button from client properties, so the field
 * needs no custom painting.
 */
public final class SearchField extends JTextField {

    /**
     * @param placeholder prompt shown while the field is empty
     * @param onChange    called with the current text after each edit
     */
    public SearchField(String placeholder, Consumer<String> onChange) {
        putClientProperty("JTextField.placeholderText", placeholder);
        putClientProperty("JTextField.showClearButton", true);
        putClientProperty("JTextField.leadingIcon",
                VectorIcon.of(VectorIcon.Glyph.SEARCH, 15, Theme.textMuted()));
        setFont(Theme.bodyFont());
        setColumns(22);

        getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                onChange.accept(getText());
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                onChange.accept(getText());
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                onChange.accept(getText());
            }
        });
    }

    @Override
    public Dimension getMaximumSize() {
        return new Dimension(320, getPreferredSize().height);
    }
}
