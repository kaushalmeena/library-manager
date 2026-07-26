package com.example.library.ui.view;

import com.example.library.ui.theme.Theme;

import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;

/**
 * Base class for the screens that fill the area to the right of the sidebar.
 *
 * <p>Provides the shared page furniture — a title, a subtitle and a row of toolbar actions — so
 * every screen has the same header geometry, and defines the {@link #refresh()} hook the shell
 * calls whenever a screen becomes visible or the underlying data changes.
 */
public abstract class View extends JPanel {

    private final JLabel titleLabel = new JLabel();
    private final JLabel subtitleLabel = new JLabel();
    private final JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, Theme.SPACE_2, 0));
    private final JPanel body = new JPanel(new BorderLayout());

    protected View(String title, String subtitle) {
        setLayout(new BorderLayout(0, Theme.SPACE_4));
        setOpaque(true);
        setBackground(Theme.canvas());
        setBorder(Theme.padding(Theme.SPACE_5, Theme.SPACE_5));

        titleLabel.setText(title);
        titleLabel.setFont(Theme.titleFont());
        titleLabel.setForeground(Theme.textPrimary());

        subtitleLabel.setText(subtitle);
        subtitleLabel.setFont(Theme.bodyFont());
        subtitleLabel.setForeground(Theme.textSecondary());

        JPanel headingText = new JPanel();
        headingText.setOpaque(false);
        headingText.setLayout(new javax.swing.BoxLayout(headingText, javax.swing.BoxLayout.Y_AXIS));
        titleLabel.setAlignmentX(LEFT_ALIGNMENT);
        subtitleLabel.setAlignmentX(LEFT_ALIGNMENT);
        headingText.add(titleLabel);
        headingText.add(Box.createVerticalStrut(2));
        headingText.add(subtitleLabel);

        actions.setOpaque(false);

        JPanel header = new JPanel(new BorderLayout(Theme.SPACE_4, 0));
        header.setOpaque(false);
        header.add(headingText, BorderLayout.WEST);
        header.add(actions, BorderLayout.EAST);

        body.setOpaque(false);

        add(header, BorderLayout.NORTH);
        add(body, BorderLayout.CENTER);
    }

    /** Adds a toolbar action to the top right of the page. */
    protected void addAction(JComponent component) {
        actions.add(component);
    }

    /** Sets the screen's main content. */
    protected void setBody(JComponent content) {
        body.removeAll();
        body.add(content, BorderLayout.CENTER);
        body.revalidate();
        body.repaint();
    }

    /** Replaces the line under the page title, used to show live counts. */
    protected void setSubtitle(String subtitle) {
        subtitleLabel.setText(subtitle);
    }

    /** Reloads the screen's data. Called when it is shown and after any change. */
    public abstract void refresh();
}
