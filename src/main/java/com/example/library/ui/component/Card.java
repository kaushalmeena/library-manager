package com.example.library.ui.component;

import com.example.library.ui.theme.Theme;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;

/**
 * A rounded, bordered surface that everything else sits on.
 *
 * <p>Painting the background itself, rather than relying on a component border, is what allows
 * the rounded corners to render cleanly against the window canvas.
 */
public class Card extends JPanel {

    private final int radius;

    public Card() {
        this(new BorderLayout(), Theme.CARD_RADIUS);
    }

    public Card(java.awt.LayoutManager layout) {
        this(layout, Theme.CARD_RADIUS);
    }

    public Card(java.awt.LayoutManager layout, int radius) {
        super(layout);
        this.radius = radius;
        setOpaque(false);
        setBorder(Theme.padding(Theme.SPACE_4));
    }

    /** A card with a heading above its content. */
    public static Card titled(String title, Component content) {
        Card card = new Card(new BorderLayout(0, Theme.SPACE_3));
        card.add(heading(title), BorderLayout.NORTH);
        card.add(content, BorderLayout.CENTER);
        return card;
    }

    /** A card with a heading, an optional action on the right, and content below. */
    public static Card titled(String title, JComponent action, Component content) {
        Card card = new Card(new BorderLayout(0, Theme.SPACE_3));
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(heading(title), BorderLayout.WEST);
        if (action != null) {
            header.add(action, BorderLayout.EAST);
        }
        card.add(header, BorderLayout.NORTH);
        card.add(content, BorderLayout.CENTER);
        return card;
    }

    /** A section heading styled consistently across every card. */
    public static JLabel heading(String text) {
        JLabel label = new JLabel(text);
        label.setFont(Theme.headingFont());
        label.setForeground(Theme.textPrimary());
        return label;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            RoundRectangle2D shape = new RoundRectangle2D.Float(
                    0.5f, 0.5f, getWidth() - 1f, getHeight() - 1f, radius, radius);
            g2.setColor(Theme.surface());
            g2.fill(shape);
            g2.setColor(Theme.border());
            g2.draw(shape);
        } finally {
            g2.dispose();
        }
        super.paintComponent(g);
    }
}
