package com.example.library.ui.theme;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.example.library.model.LoanStatus;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import java.awt.Color;
import java.awt.Font;
import java.awt.Window;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

/**
 * The single source of truth for how the application looks: look and feel installation,
 * the colour palette, type scale and spacing steps.
 *
 * <p>Colours come in light and dark pairs so every screen can ask for a semantic colour, such
 * as {@link #danger()}, without caring which mode is active. The chosen mode is remembered
 * between runs.
 */
public final class Theme {

    /** Spacing scale, in pixels. Layout code uses these instead of ad-hoc numbers. */
    public static final int SPACE_1 = 4;
    public static final int SPACE_2 = 8;
    public static final int SPACE_3 = 12;
    public static final int SPACE_4 = 16;
    public static final int SPACE_5 = 24;
    public static final int SPACE_6 = 32;

    /** Corner radius for cards and other large surfaces. */
    public static final int CARD_RADIUS = 14;

    public static final int TABLE_ROW_HEIGHT = 34;

    private static final String PREF_DARK = "darkMode";

    private static final Preferences PREFERENCES =
            Preferences.userRoot().node("com/example/library");

    private static final List<Runnable> LISTENERS = new ArrayList<>();

    private static boolean dark;

    private Theme() {
    }

    /** Installs the look and feel and applies the shared component defaults. */
    public static void install() {
        dark = PREFERENCES.getBoolean(PREF_DARK, false);
        applyLookAndFeel();
    }

    /** Switches between light and dark, restyling every open window. */
    public static void toggleDarkMode() {
        dark = !dark;
        PREFERENCES.putBoolean(PREF_DARK, dark);
        applyLookAndFeel();
        for (Window window : Window.getWindows()) {
            SwingUtilities.updateComponentTreeUI(window);
        }
        LISTENERS.forEach(Runnable::run);
    }

    /**
     * Registers a callback for theme changes, for components that paint their own colours and
     * therefore cannot rely on {@code updateComponentTreeUI} alone.
     */
    public static void onThemeChange(Runnable listener) {
        LISTENERS.add(listener);
    }

    public static boolean isDark() {
        return dark;
    }

    private static void applyLookAndFeel() {
        if (dark) {
            FlatDarkLaf.setup();
        } else {
            FlatLightLaf.setup();
        }
        applyDefaults();
    }

    /** Rounds off the stock FlatLaf components and sets the shared metrics. */
    private static void applyDefaults() {
        UIManager.put("Component.arc", 10);
        UIManager.put("Button.arc", 10);
        UIManager.put("TextComponent.arc", 10);
        UIManager.put("ProgressBar.arc", 10);
        UIManager.put("CheckBox.arc", 6);
        UIManager.put("ScrollBar.thumbArc", 999);
        UIManager.put("ScrollBar.thumbInsets", new java.awt.Insets(2, 2, 2, 2));
        UIManager.put("ScrollBar.width", 12);
        UIManager.put("ScrollBar.showButtons", false);

        UIManager.put("Component.focusWidth", 1);
        UIManager.put("Component.innerFocusWidth", 1);
        UIManager.put("Component.focusColor", accent());
        UIManager.put("Component.focusedBorderColor", accent());

        // Default buttons are deliberately left to the look and feel. Overriding only their
        // foreground here produced white text on the pale default background of message
        // dialogs; the application's own primary buttons set both colours together instead,
        // through Buttons.primary and asPrimaryButton.
        UIManager.put("Button.default.focusedBorderColor", accent());

        UIManager.put("Table.rowHeight", TABLE_ROW_HEIGHT);
        UIManager.put("Table.showHorizontalLines", true);
        UIManager.put("Table.showVerticalLines", false);
        UIManager.put("Table.intercellSpacing", new java.awt.Dimension(0, 1));
        UIManager.put("Table.gridColor", divider());
        UIManager.put("Table.selectionBackground", accentSoft());
        UIManager.put("Table.selectionForeground", textPrimary());
        UIManager.put("Table.alternateRowColor", null);
        UIManager.put("TableHeader.height", 34);
        UIManager.put("TableHeader.separatorColor", divider());
        UIManager.put("TableHeader.bottomSeparatorColor", divider());
        UIManager.put("TableHeader.foreground", textMuted());
        UIManager.put("TableHeader.background", surface());

        UIManager.put("TitlePane.unifiedBackground", true);
        UIManager.put("ToolTip.background", dark ? new Color(0x2B2F3A) : new Color(0x1F2430));
        UIManager.put("ToolTip.foreground", dark ? new Color(0xE6E8EE) : Color.WHITE);
        UIManager.put("OptionPane.showIcon", true);
    }

    // ---------------------------------------------------------------- palette

    private static Color pick(int light, int darkValue) {
        return new Color(dark ? darkValue : light);
    }

    /** Brand colour, used for primary actions and the active navigation item. */
    public static Color accent() {
        return pick(0x4F46E5, 0x7C7CF7);
    }

    public static Color accentHover() {
        return pick(0x4338CA, 0x8E8EF9);
    }

    public static Color accentPressed() {
        return pick(0x3730A3, 0x6A6AF0);
    }

    /** A wash of the accent, for selected rows and tinted icon wells. */
    public static Color accentSoft() {
        return pick(0xE8E7FD, 0x2E2C55);
    }

    /** The window background, a shade behind {@link #surface()}. */
    public static Color canvas() {
        return pick(0xF6F7FB, 0x14161C);
    }

    /** Card and panel background. */
    public static Color surface() {
        return pick(0xFFFFFF, 0x1D2029);
    }

    /** A slightly recessed surface, used for the sidebar and table headers. */
    public static Color surfaceSunken() {
        return pick(0xFFFFFF, 0x191C24);
    }

    public static Color border() {
        return pick(0xE3E6EF, 0x2C313D);
    }

    public static Color divider() {
        return pick(0xEDEFF5, 0x262A34);
    }

    public static Color textPrimary() {
        return pick(0x161923, 0xE8EAF0);
    }

    public static Color textSecondary() {
        return pick(0x4A5065, 0xA9AFC0);
    }

    public static Color textMuted() {
        return pick(0x8A90A6, 0x767D91);
    }

    public static Color success() {
        return pick(0x0F9D58, 0x3DD68C);
    }

    public static Color warning() {
        return pick(0xC77700, 0xF0B429);
    }

    public static Color danger() {
        return pick(0xD92D20, 0xF97066);
    }

    public static Color info() {
        return pick(0x1570EF, 0x63A8FF);
    }

    /** The colour for a loan status badge. */
    public static Color statusColor(LoanStatus status) {
        return toneColor(status.tone());
    }

    /** Resolves a semantic tone to a colour. */
    public static Color toneColor(LoanStatus.Tone tone) {
        return switch (tone) {
            case SUCCESS -> success();
            case WARNING -> warning();
            case DANGER -> danger();
            case INFO -> info();
            case NEUTRAL -> textMuted();
        };
    }

    /** A translucent wash of {@code base}, for badge and chip backgrounds. */
    public static Color tint(Color base) {
        return new Color(base.getRed(), base.getGreen(), base.getBlue(), dark ? 46 : 30);
    }

    // ------------------------------------------------------------------- type

    private static Font base() {
        Font font = UIManager.getFont("defaultFont");
        return font != null ? font : new Font(Font.SANS_SERIF, Font.PLAIN, 13);
    }

    /** Page and dialog titles. */
    public static Font titleFont() {
        return base().deriveFont(Font.BOLD, 21f);
    }

    /** Card headings and section titles. */
    public static Font headingFont() {
        return base().deriveFont(Font.BOLD, 15f);
    }

    /** The oversized number on a stat card. */
    public static Font metricFont() {
        return base().deriveFont(Font.BOLD, 28f);
    }

    public static Font bodyFont() {
        return base().deriveFont(Font.PLAIN, 13f);
    }

    public static Font bodyBoldFont() {
        return base().deriveFont(Font.BOLD, 13f);
    }

    /** Captions, table headers and helper text. */
    public static Font smallFont() {
        return base().deriveFont(Font.PLAIN, 11.5f);
    }

    public static Font smallBoldFont() {
        return base().deriveFont(Font.BOLD, 11.5f);
    }

    // ---------------------------------------------------------------- helpers

    /** Uniform padding on every side. */
    public static Border padding(int all) {
        return new EmptyBorder(all, all, all, all);
    }

    public static Border padding(int vertical, int horizontal) {
        return new EmptyBorder(vertical, horizontal, vertical, horizontal);
    }

    public static Border padding(int top, int left, int bottom, int right) {
        return new EmptyBorder(top, left, bottom, right);
    }

    /**
     * Marks a button as the accented primary action.
     *
     * <p>Both the ordinary and the default-button colours are set. A primary button is often also
     * the root pane's default button, and FlatLaf paints that state from its {@code default.*}
     * colours — styling only the ordinary ones left such a button unpainted.
     */
    public static void asPrimaryButton(JComponent button) {
        String accent = hex(accent());
        String hover = hex(accentHover());
        String pressed = hex(accentPressed());
        button.putClientProperty("FlatLaf.style",
                "background: " + accent + "; foreground: #FFFFFF;"
                        + " hoverBackground: " + hover + ";"
                        + " pressedBackground: " + pressed + ";"
                        + " borderColor: " + accent + ";"
                        + " focusedBorderColor: " + accent + ";"
                        + " default.background: " + accent + ";"
                        + " default.foreground: #FFFFFF;"
                        + " default.hoverBackground: " + hover + ";"
                        + " default.pressedBackground: " + pressed + ";"
                        + " default.borderWidth: 0");
    }

    /** Marks a button as a quiet, borderless action. */
    public static void asQuietButton(JComponent button) {
        button.putClientProperty("JButton.buttonType", "borderless");
    }

    /** Marks a button as a destructive action. */
    public static void asDangerButton(JComponent button) {
        button.putClientProperty("FlatLaf.style",
                "foreground: " + hex(danger()) + "; borderColor: " + hex(danger()) + ";"
                        + " focusedBorderColor: " + hex(danger()) + ";"
                        + " hoverBorderColor: " + hex(danger()) + ";");
    }

    /** Formats a colour the way FlatLaf's style strings expect. */
    public static String hex(Color color) {
        return String.format("#%02X%02X%02X", color.getRed(), color.getGreen(), color.getBlue());
    }
}
