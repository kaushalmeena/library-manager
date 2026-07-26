package com.example.library.ui;

import com.example.library.LibraryServices;
import com.example.library.model.User;
import com.example.library.ui.support.Dialogs;
import com.example.library.ui.theme.Theme;
import com.example.library.ui.theme.VectorIcon;
import com.example.library.ui.view.CatalogueView;
import com.example.library.ui.view.CirculationView;
import com.example.library.ui.view.DashboardView;
import com.example.library.ui.view.MembersView;
import com.example.library.ui.view.ProfileView;
import com.example.library.ui.view.View;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The application shell: a navigation rail on the left and one screen at a time on the right.
 *
 * <p>Which destinations exist depends on the signed-in role, so a student never sees the staff
 * screens rather than seeing them disabled. Screens are created once and swapped with a
 * {@link CardLayout}; each is refreshed as it comes into view so it never shows stale data.
 */
public final class MainFrame extends JFrame {

    private static final String DASHBOARD = "dashboard";
    private static final String CATALOGUE = "catalogue";
    private static final String CIRCULATION = "circulation";
    private static final String MEMBERS = "members";
    private static final String PROFILE = "profile";

    private final LibraryServices services;
    private final Map<String, View> views = new LinkedHashMap<>();
    private final CardLayout cards = new CardLayout();
    private final JPanel content = new JPanel(cards);

    private User account;
    private Sidebar sidebar;

    public MainFrame(LibraryServices services, User account) {
        super("Library Manager");
        this.services = services;
        this.account = account;

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setIconImages(com.example.library.ui.theme.AppIcon.images());
        setSize(preferredWindowSize());
        setMinimumSize(new Dimension(1040, 660));
        setLocationRelativeTo(null);

        buildContent();
        navigateTo(DASHBOARD);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                confirmExit();
            }
        });

        Theme.onThemeChange(this::applyTheme);
    }

    /**
     * The size the window opens at: large enough that the dashboard fits without scrolling, but
     * never larger than the screen it is opening on.
     */
    private static Dimension preferredWindowSize() {
        java.awt.Rectangle screen = java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getMaximumWindowBounds();
        return new Dimension(
                Math.min(1500, Math.max(1040, screen.width - 80)),
                Math.min(940, Math.max(660, screen.height - 80)));
    }

    private void buildContent() {
        sidebar = new Sidebar(account, this::navigateTo, this::signOut);

        content.setOpaque(true);
        content.setBackground(Theme.canvas());

        // Every role gets a dashboard, but its contents differ.
        addView(DASHBOARD, "Dashboard", VectorIcon.Glyph.DASHBOARD,
                new DashboardView(services, account, this::navigateTo));

        addView(CATALOGUE, "Catalogue", VectorIcon.Glyph.BOOKS,
                new CatalogueView(services, account, this::refreshAll));

        addView(CIRCULATION, account.role().canSeeAllLoans() ? "Circulation" : "My loans",
                VectorIcon.Glyph.CIRCULATION,
                new CirculationView(services, account, this::refreshAll));

        if (account.role().canManageUsers()) {
            addView(MEMBERS, "Members", VectorIcon.Glyph.MEMBERS,
                    new MembersView(services, account, this::refreshAll));
        }

        addView(PROFILE, "Profile", VectorIcon.Glyph.PROFILE,
                new ProfileView(services, account, this::onAccountUpdated));

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Theme.canvas());
        root.add(sidebar, BorderLayout.WEST);
        root.add(content, BorderLayout.CENTER);
        setContentPane(root);
    }

    private void addView(String key, String label, VectorIcon.Glyph glyph, View view) {
        views.put(key, view);
        content.add(view, key);
        sidebar.addItem(key, label, glyph);
    }

    /** Shows a screen, refreshing it first so it never appears with stale numbers. */
    public void navigateTo(String key) {
        View view = views.get(key);
        if (view == null) {
            return;
        }
        try {
            view.refresh();
        } catch (RuntimeException e) {
            Dialogs.showError(this, "Could not load that screen.", e);
        }
        cards.show(content, key);
        sidebar.setActive(key);
        updateOverdueBadge();
    }

    /**
     * Reloads every screen. Called after any change that could affect more than one of them,
     * such as issuing a book, which alters both the catalogue and the circulation list.
     */
    public void refreshAll() {
        views.values().forEach(view -> {
            try {
                view.refresh();
            } catch (RuntimeException e) {
                Dialogs.showError(this, "Could not reload the screens.", e);
            }
        });
        updateOverdueBadge();
    }

    /** Puts the count of overdue loans next to the circulation destination. */
    private void updateOverdueBadge() {
        if (!account.role().canSeeAllLoans()) {
            sidebar.setBadgeCount(CIRCULATION,
                    services.circulationService().countOverdueForUser(account.id()));
            return;
        }
        sidebar.setBadgeCount(CIRCULATION, services.circulationService().overdueLoans().size());
    }

    /** Rebuilds the shell after the person edits their own account. */
    private void onAccountUpdated(User updated) {
        this.account = updated;
        rebuildShell();
    }

    /**
     * Reacts to a theme change by rebuilding the shell.
     *
     * <p>Screens set their colours when they are constructed — a label's foreground, an icon's
     * stroke colour — and Swing has no way to revisit those after the fact, so restyling in place
     * left dark text on a dark background. Rebuilding is instant here and guarantees every
     * component picks up the new palette.
     */
    private void applyTheme() {
        // A signed-out frame is still registered as a listener; it must not rebuild itself.
        if (!isDisplayable()) {
            return;
        }
        rebuildShell();
    }

    /** Recreates the sidebar and every screen, returning to the destination that was open. */
    private void rebuildShell() {
        String active = sidebar == null ? DASHBOARD : sidebar.activeKey();
        views.clear();
        content.removeAll();
        buildContent();
        navigateTo(views.containsKey(active) ? active : DASHBOARD);
        SwingUtilities.updateComponentTreeUI(this);
        revalidate();
        repaint();
    }

    private void signOut() {
        if (!Dialogs.confirm(this, "Sign out",
                "Sign out of Library Manager? Any unsaved text in a form will be lost.",
                "Sign out")) {
            return;
        }
        dispose();
        LoginFrame login = new LoginFrame(services);
        login.setVisible(true);
        login.focusFirstField();
    }

    private void confirmExit() {
        if (Dialogs.confirm(this, "Quit", "Close Library Manager?", "Quit")) {
            dispose();
            System.exit(0);
        }
    }
}
