package com.example.library.ui.component;

import com.example.library.model.LoanStatus;
import com.example.library.ui.theme.Theme;

import javax.swing.JLabel;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;

/**
 * A small pill showing a status or a role, tinted to match its meaning. Used both standalone
 * and as the renderer for status columns in tables.
 */
public class Badge extends JLabel {

    private static final int PADDING_X = 9;
    private static final int PADDING_Y = 3;

    private Color tone;

    public Badge(String text, Color tone) {
        super(text);
        this.tone = tone;
        setFont(Theme.smallBoldFont());
        setOpaque(false);
        setBorder(Theme.padding(PADDING_Y, PADDING_X));
        setForeground(tone);
    }

    /** A badge coloured for a loan status. */
    public static Badge forStatus(LoanStatus status) {
        return new Badge(status.label(), Theme.statusColor(status));
    }

    /** A neutral badge, e.g. for a role or a count. */
    public static Badge neutral(String text) {
        return new Badge(text, Theme.textSecondary());
    }

    public void setTone(Color newTone) {
        this.tone = newTone;
        setForeground(newTone);
    }

    public Color tone() {
        return tone;
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension size = super.getPreferredSize();
        return new Dimension(size.width, Math.max(size.height, 22));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            int height = getHeight();
            g2.setColor(Theme.tint(tone));
            g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), height, height, height));
        } finally {
            g2.dispose();
        }
        super.paintComponent(g);
    }
}
