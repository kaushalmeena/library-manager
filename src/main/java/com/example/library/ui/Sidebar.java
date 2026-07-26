package com.example.library.ui;

import com.example.library.model.User;
import com.example.library.ui.theme.Theme;
import com.example.library.ui.theme.VectorIcon;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * The left navigation rail: product mark at the top, the destinations the signed-in role may
 * visit in the middle, and the account block with the theme toggle and sign-out at the bottom.
 */
public final class Sidebar extends JPanel {

    private static final int WIDTH = 232;

    private final Map<String, NavItem> items = new LinkedHashMap<>();
    private final JPanel navigation = new JPanel();
    private final Consumer<String> onNavigate;

    private JButton themeToggle;
    private String activeKey;

    /**
     * @param account    the signed-in account, shown in the footer
     * @param onNavigate called with a destination key when a nav item is chosen
     * @param onSignOut  called when sign out is chosen
     */
    public Sidebar(User account, Consumer<String> onNavigate, Runnable onSignOut) {
        this.onNavigate = onNavigate;

        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(WIDTH, 0));
        setBorder(Theme.padding(Theme.SPACE_4, Theme.SPACE_3));
        setOpaque(true);
        setBackground(Theme.surfaceSunken());

        navigation.setOpaque(false);
        navigation.setLayout(new BoxLayout(navigation, BoxLayout.Y_AXIS));

        add(buildBrand(), BorderLayout.NORTH);
        add(navigation, BorderLayout.CENTER);
        add(buildFooter(account, onSignOut), BorderLayout.SOUTH);
    }

    private JPanel buildBrand() {
        JLabel mark = new JLabel("Library",
                VectorIcon.bold(VectorIcon.Glyph.LIBRARY, 22, Theme.accent()),
                SwingConstants.LEFT);
        mark.setFont(Theme.headingFont().deriveFont(17f));
        mark.setForeground(Theme.textPrimary());
        mark.setIconTextGap(Theme.SPACE_2);

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.setBorder(Theme.padding(Theme.SPACE_2, Theme.SPACE_3, Theme.SPACE_5,
                Theme.SPACE_3));
        wrapper.add(mark, BorderLayout.WEST);
        return wrapper;
    }

    private JPanel buildFooter(User account, Runnable onSignOut) {
        JPanel footer = new JPanel();
        footer.setOpaque(false);
        footer.setLayout(new BoxLayout(footer, BoxLayout.Y_AXIS));

        JPanel accountRow = new JPanel(new BorderLayout(Theme.SPACE_2, 0));
        accountRow.setOpaque(false);
        accountRow.setBorder(Theme.padding(Theme.SPACE_2, Theme.SPACE_2));
        accountRow.add(new Avatar(account.initials()), BorderLayout.WEST);

        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        JLabel name = new JLabel(account.name());
        name.setFont(Theme.bodyBoldFont());
        name.setForeground(Theme.textPrimary());
        JLabel role = new JLabel(account.role().displayName());
        role.setFont(Theme.smallFont());
        role.setForeground(Theme.textMuted());
        name.setAlignmentX(LEFT_ALIGNMENT);
        role.setAlignmentX(LEFT_ALIGNMENT);
        text.add(name);
        text.add(role);
        accountRow.add(text, BorderLayout.CENTER);

        themeToggle = footerButton("", VectorIcon.Glyph.MOON);
        themeToggle.addActionListener(e -> Theme.toggleDarkMode());
        applyThemeToggleLabel();

        JButton signOut = footerButton("Sign out", VectorIcon.Glyph.LOGOUT);
        signOut.addActionListener(e -> onSignOut.run());

        footer.add(new Separator());
        footer.add(Box.createVerticalStrut(Theme.SPACE_2));
        footer.add(accountRow);
        footer.add(Box.createVerticalStrut(Theme.SPACE_1));
        footer.add(themeToggle);
        footer.add(signOut);

        for (Component child : footer.getComponents()) {
            if (child instanceof javax.swing.JComponent jc) {
                jc.setAlignmentX(LEFT_ALIGNMENT);
                jc.setMaximumSize(new Dimension(Integer.MAX_VALUE,
                        jc.getPreferredSize().height));
            }
        }
        return footer;
    }

    private JButton footerButton(String text, VectorIcon.Glyph glyph) {
        JButton button = new JButton(text, VectorIcon.of(glyph, 16, Theme.textSecondary()));
        button.setFont(Theme.bodyFont());
        button.setForeground(Theme.textSecondary());
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setIconTextGap(Theme.SPACE_3);
        button.setBorder(Theme.padding(Theme.SPACE_2, Theme.SPACE_2));
        button.setFocusPainted(false);
        Theme.asQuietButton(button);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
    }

    /**
     * Labels the toggle with the mode it switches to, which has to be reapplied after every
     * theme change or the button keeps advertising the mode already in use.
     */
    private void applyThemeToggleLabel() {
        boolean dark = Theme.isDark();
        themeToggle.setText(dark ? "Light mode" : "Dark mode");
        themeToggle.setIcon(VectorIcon.of(dark ? VectorIcon.Glyph.SUN : VectorIcon.Glyph.MOON,
                16, Theme.textSecondary()));
    }

    /**
     * Appends a destination.
     *
     * @param key   the identifier passed back to the navigation callback
     * @param label the visible label
     * @param glyph the icon
     */
    public void addItem(String key, String label, VectorIcon.Glyph glyph) {
        NavItem item = new NavItem(key, label, glyph);
        items.put(key, item);
        navigation.add(item);
        navigation.add(Box.createVerticalStrut(2));
    }

    /** Highlights a destination without firing the navigation callback. */
    public void setActive(String key) {
        this.activeKey = key;
        items.forEach((itemKey, item) -> item.setActive(itemKey.equals(key)));
    }

    /** Shows a count beside a destination, e.g. the number of overdue loans. */
    public void setBadgeCount(String key, int count) {
        NavItem item = items.get(key);
        if (item != null) {
            item.setBadgeCount(count);
        }
    }
    /** A single navigation row, painted as a rounded pill when active. */
    private final class NavItem extends JPanel {

        private final String key;
        private final String label;
        private final VectorIcon.Glyph glyph;
        private final JLabel textLabel = new JLabel();
        private final JLabel countLabel = new JLabel();

        private boolean active;
        private boolean hovered;

        NavItem(String key, String label, VectorIcon.Glyph glyph) {
            this.key = key;
            this.label = label;
            this.glyph = glyph;

            setLayout(new BorderLayout(Theme.SPACE_3, 0));
            setOpaque(false);
            setBorder(Theme.padding(9, Theme.SPACE_3));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            textLabel.setText(label);
            textLabel.setFont(Theme.bodyFont());
            countLabel.setFont(Theme.smallBoldFont());
            countLabel.setVisible(false);

            add(textLabel, BorderLayout.CENTER);
            add(countLabel, BorderLayout.EAST);
            applyColours();

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    onNavigate.accept(key);
                }

                @Override
                public void mouseEntered(MouseEvent e) {
                    hovered = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hovered = false;
                    repaint();
                }
            });
        }

        void setActive(boolean nowActive) {
            this.active = nowActive;
            applyColours();
            repaint();
        }

        void setBadgeCount(int count) {
            countLabel.setVisible(count > 0);
            countLabel.setText(count > 99 ? "99+" : Integer.toString(count));
            countLabel.setForeground(Theme.danger());
        }
        private void applyColours() {
            Color foreground = active ? Theme.accent() : Theme.textSecondary();
            textLabel.setForeground(foreground);
            textLabel.setFont(active ? Theme.bodyBoldFont() : Theme.bodyFont());
            setIcon(VectorIcon.of(glyph, 18, foreground));
        }

        private void setIcon(VectorIcon icon) {
            textLabel.setIcon(icon);
            textLabel.setIconTextGap(Theme.SPACE_3);
        }

        @Override
        public Dimension getMaximumSize() {
            return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
        }

        @Override
        protected void paintComponent(Graphics g) {
            if (active || hovered) {
                Graphics2D g2 = (Graphics2D) g.create();
                try {
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                            RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(active ? Theme.accentSoft() : Theme.divider());
                    g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 10, 10));
                } finally {
                    g2.dispose();
                }
            }
            super.paintComponent(g);
        }

        @Override
        public String toString() {
            return label + " (" + key + ")";
        }
    }

    /** The signed-in account's initials in an accent-tinted circle. */
    private static final class Avatar extends JPanel {

        private static final int DIAMETER = 34;

        private final String initials;

        Avatar(String initials) {
            this.initials = initials;
            setOpaque(false);
            setPreferredSize(new Dimension(DIAMETER, DIAMETER));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                        RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g2.setColor(Theme.accent());
                g2.fill(new Ellipse2D.Float(0, 0, DIAMETER, DIAMETER));
                g2.setColor(Color.WHITE);
                g2.setFont(Theme.smallBoldFont());
                var metrics = g2.getFontMetrics();
                int x = (DIAMETER - metrics.stringWidth(initials)) / 2;
                int y = (DIAMETER - metrics.getHeight()) / 2 + metrics.getAscent();
                g2.drawString(initials, x, y);
            } finally {
                g2.dispose();
            }
        }
    }

    /** A one pixel rule used to separate the footer from the destinations. */
    private static final class Separator extends JPanel {

        Separator() {
            setOpaque(false);
            setPreferredSize(new Dimension(0, 1));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        }

        @Override
        protected void paintComponent(Graphics g) {
            g.setColor(Theme.border());
            g.fillRect(Theme.SPACE_2, 0, getWidth() - Theme.SPACE_4, 1);
        }
    }

    /** The currently highlighted destination. */
    public String activeKey() {
        return activeKey;
    }
}
