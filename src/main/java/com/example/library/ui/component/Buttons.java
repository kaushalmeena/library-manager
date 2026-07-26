package com.example.library.ui.component;

import com.example.library.ui.theme.Theme;
import com.example.library.ui.theme.VectorIcon;

import javax.swing.JButton;
import java.awt.Color;

/**
 * Builds the application's buttons.
 *
 * <p>Each style pairs a background with an icon colour that reads against it. Going through this
 * class rather than styling buttons at each call site is what stops an accent-coloured icon being
 * placed on an accent-coloured background, where it disappears entirely.
 */
public final class Buttons {

    private static final int ICON_SIZE = 15;

    private Buttons() {
    }

    /** The screen's main action: accent background, white label and icon. */
    public static JButton primary(String text, VectorIcon.Glyph glyph) {
        JButton button = base(text, glyph, Color.WHITE);
        button.setFont(Theme.bodyBoldFont());
        Theme.asPrimaryButton(button);
        return button;
    }

    /** A secondary action: default background, muted icon. */
    public static JButton secondary(String text, VectorIcon.Glyph glyph) {
        JButton button = base(text, glyph, Theme.textSecondary());
        button.setFont(Theme.bodyFont());
        return button;
    }

    /** A destructive action: red label, border and icon. */
    public static JButton danger(String text, VectorIcon.Glyph glyph) {
        JButton button = base(text, glyph, Theme.danger());
        button.setFont(Theme.bodyFont());
        Theme.asDangerButton(button);
        return button;
    }

    /** A secondary action whose icon carries a status colour, such as a green tick. */
    public static JButton tinted(String text, VectorIcon.Glyph glyph, Color iconColor) {
        JButton button = base(text, glyph, iconColor);
        button.setFont(Theme.bodyFont());
        return button;
    }

    private static JButton base(String text, VectorIcon.Glyph glyph, Color iconColor) {
        JButton button = glyph == null
                ? new JButton(text)
                : new JButton(text, VectorIcon.of(glyph, ICON_SIZE, iconColor));
        button.setIconTextGap(Theme.SPACE_2);
        return button;
    }
}
