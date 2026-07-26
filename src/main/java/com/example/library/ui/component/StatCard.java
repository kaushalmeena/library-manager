package com.example.library.ui.component;

import com.example.library.ui.theme.Theme;
import com.example.library.ui.theme.VectorIcon;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;

/**
 * A dashboard tile: a large number, what it counts, a supporting line, and a tinted icon well.
 *
 * <p>The value can be replaced after construction so a refresh does not have to rebuild the
 * dashboard's layout.
 */
public final class StatCard extends Card {

    private final JLabel valueLabel = new JLabel("—");
    private final JLabel captionLabel = new JLabel(" ");
    private final IconWell iconWell;

    /**
     * @param title   what the number counts, e.g. "On loan"
     * @param glyph   the icon shown in the tinted well
     * @param tone    the colour family for the well
     */
    public StatCard(String title, VectorIcon.Glyph glyph, Color tone) {
        super(new BorderLayout(Theme.SPACE_3, 0));
        setBorder(Theme.padding(Theme.SPACE_4, Theme.SPACE_4));

        JLabel titleLabel = new JLabel(title.toUpperCase(java.util.Locale.ROOT));
        titleLabel.setFont(Theme.smallBoldFont());
        titleLabel.setForeground(Theme.textMuted());

        valueLabel.setFont(Theme.metricFont());
        valueLabel.setForeground(Theme.textPrimary());

        captionLabel.setFont(Theme.smallFont());
        captionLabel.setForeground(Theme.textSecondary());

        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        text.add(titleLabel);
        text.add(Box.createVerticalStrut(Theme.SPACE_1));
        text.add(valueLabel);
        text.add(Box.createVerticalStrut(2));
        text.add(captionLabel);
        for (java.awt.Component child : text.getComponents()) {
            if (child instanceof JLabel label) {
                label.setAlignmentX(LEFT_ALIGNMENT);
            }
        }

        iconWell = new IconWell(glyph, tone);

        add(text, BorderLayout.CENTER);
        add(iconWell, BorderLayout.EAST);
    }

    /** Replaces the headline number. */
    public StatCard setValue(String value) {
        valueLabel.setText(value);
        return this;
    }

    /** Replaces the supporting line under the number. */
    public StatCard setCaption(String caption) {
        captionLabel.setText(caption == null || caption.isBlank() ? " " : caption);
        return this;
    }
    @Override
    public Dimension getPreferredSize() {
        Dimension size = super.getPreferredSize();
        return new Dimension(Math.max(size.width, 190), Math.max(size.height, 104));
    }

    /** A circle of tinted colour with an icon centred in it. */
    private static final class IconWell extends JPanel {

        private static final int DIAMETER = 44;
        private static final int ICON_SIZE = 22;

        private final VectorIcon.Glyph glyph;
        private final Color tone;
        private VectorIcon icon;

        IconWell(VectorIcon.Glyph glyph, Color tone) {
            this.glyph = glyph;
            this.tone = tone;
            this.icon = VectorIcon.of(glyph, ICON_SIZE, tone);
            setOpaque(false);
            setPreferredSize(new Dimension(DIAMETER, DIAMETER));
        }
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                int x = (getWidth() - DIAMETER) / 2;
                int y = (getHeight() - DIAMETER) / 2;
                g2.setColor(Theme.tint(tone));
                g2.fill(new Ellipse2D.Float(x, y, DIAMETER, DIAMETER));
                icon.paintIcon(this, g2, x + (DIAMETER - ICON_SIZE) / 2,
                        y + (DIAMETER - ICON_SIZE) / 2);
            } finally {
                g2.dispose();
            }
        }
    }
}
