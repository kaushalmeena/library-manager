package com.example.library.ui;

import com.example.library.LibraryServices;
import com.example.library.model.User;
import com.example.library.ui.component.Card;
import com.example.library.ui.support.Dialogs;
import com.example.library.ui.theme.Theme;
import com.example.library.ui.theme.VectorIcon;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GradientPaint;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.util.Optional;

/**
 * The sign-in window: a branded panel on the left, the credentials form on the right.
 *
 * <p>On success this frame closes and hands control to {@link MainFrame}, so only one of the two
 * is ever on screen.
 */
public final class LoginFrame extends JFrame {

    private static final int WINDOW_WIDTH = 860;
    private static final int WINDOW_HEIGHT = 520;

    private final LibraryServices services;
    private final JTextField handleField = new JTextField(18);
    private final JPasswordField passwordField = new JPasswordField(18);
    private final JLabel errorLabel = new JLabel(" ");

    private JButton signInButton;

    public LoginFrame(LibraryServices services) {
        super("Library Manager — Sign in");
        this.services = services;

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setIconImages(com.example.library.ui.theme.AppIcon.images());
        setSize(WINDOW_WIDTH, WINDOW_HEIGHT);
        setMinimumSize(new Dimension(720, 480));
        setLocationRelativeTo(null);
        setContentPane(buildContent());
        getRootPane().setDefaultButton(signInButton);
    }

    private JPanel buildContent() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Theme.canvas());
        root.add(new BrandPanel(), BorderLayout.WEST);
        root.add(buildForm(), BorderLayout.CENTER);
        return root;
    }

    private JPanel buildForm() {
        JPanel wrapper = new JPanel(new java.awt.GridBagLayout());
        wrapper.setBackground(Theme.canvas());

        Card card = new Card();
        card.setBorder(Theme.padding(Theme.SPACE_6, Theme.SPACE_6));
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setPreferredSize(new Dimension(360, 400));

        JLabel title = new JLabel("Welcome back");
        title.setFont(Theme.titleFont());
        title.setForeground(Theme.textPrimary());

        JLabel subtitle = new JLabel("Sign in to manage the library.");
        subtitle.setFont(Theme.bodyFont());
        subtitle.setForeground(Theme.textSecondary());

        handleField.putClientProperty("JTextField.placeholderText", "username or email");
        handleField.setFont(Theme.bodyFont());
        passwordField.putClientProperty("JTextField.placeholderText", "your password");
        passwordField.putClientProperty("JPasswordField.showRevealButton", true);
        passwordField.setFont(Theme.bodyFont());

        errorLabel.setFont(Theme.smallFont());
        errorLabel.setForeground(Theme.danger());

        signInButton = new JButton("Sign in");
        signInButton.setFont(Theme.bodyBoldFont());
        signInButton.addActionListener(this::onSignIn);
        signInButton.setPreferredSize(new Dimension(120, 38));
        Theme.asPrimaryButton(signInButton);

        JButton registerButton = new JButton("Create an account");
        registerButton.setFont(Theme.bodyFont());
        registerButton.addActionListener(e -> openRegistration());
        Theme.asQuietButton(registerButton);

        JButton themeButton = new JButton("Switch theme",
                VectorIcon.of(Theme.isDark() ? VectorIcon.Glyph.SUN : VectorIcon.Glyph.MOON, 15,
                        Theme.textSecondary()));
        themeButton.setFont(Theme.smallFont());
        Theme.asQuietButton(themeButton);
        themeButton.addActionListener(e -> {
            Theme.toggleDarkMode();
            setContentPane(buildContent());
            getRootPane().setDefaultButton(signInButton);
            revalidate();
            repaint();
        });

        card.add(alignLeft(title));
        card.add(Box.createVerticalStrut(Theme.SPACE_2));
        card.add(alignLeft(subtitle));
        card.add(Box.createVerticalStrut(Theme.SPACE_5));
        card.add(alignLeft(fieldLabel("Username or email")));
        card.add(Box.createVerticalStrut(Theme.SPACE_1));
        card.add(alignLeft(sized(handleField)));
        card.add(Box.createVerticalStrut(Theme.SPACE_3));
        card.add(alignLeft(fieldLabel("Password")));
        card.add(Box.createVerticalStrut(Theme.SPACE_1));
        card.add(alignLeft(sized(passwordField)));
        card.add(Box.createVerticalStrut(Theme.SPACE_2));
        card.add(alignLeft(errorLabel));
        card.add(Box.createVerticalStrut(Theme.SPACE_3));
        card.add(alignLeft(sized(signInButton)));
        card.add(Box.createVerticalStrut(Theme.SPACE_2));
        card.add(alignLeft(registerButton));
        card.add(Box.createVerticalGlue());
        card.add(alignLeft(themeButton));

        wrapper.add(card);
        return wrapper;
    }

    private static JLabel fieldLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(Theme.smallBoldFont());
        label.setForeground(Theme.textSecondary());
        return label;
    }

    /** Stops BoxLayout stretching a field to the full card height. */
    private static <C extends Component> C sized(C component) {
        Dimension preferred = component.getPreferredSize();
        component.setMaximumSize(new Dimension(Integer.MAX_VALUE, preferred.height));
        return component;
    }

    private static Component alignLeft(Component component) {
        if (component instanceof javax.swing.JComponent jc) {
            jc.setAlignmentX(Component.LEFT_ALIGNMENT);
        }
        return component;
    }

    private void onSignIn(ActionEvent event) {
        errorLabel.setText(" ");
        String handle = handleField.getText();
        char[] password = passwordField.getPassword();
        try {
            Optional<User> account = services.authService().authenticate(handle, password);
            if (account.isEmpty()) {
                errorLabel.setText("That username or password is not right.");
                passwordField.selectAll();
                passwordField.requestFocusInWindow();
                return;
            }
            openMainWindow(account.get());
        } catch (RuntimeException e) {
            Dialogs.showError(this, "Could not sign in.", e);
        } finally {
            java.util.Arrays.fill(password, '\0');
        }
    }

    private void openRegistration() {
        RegisterDialog dialog = new RegisterDialog(this, services);
        dialog.setVisible(true);
        dialog.registeredUsername().ifPresent(username -> {
            handleField.setText(username);
            passwordField.setText("");
            passwordField.requestFocusInWindow();
            errorLabel.setText(" ");
        });
    }

    private void openMainWindow(User account) {
        dispose();
        MainFrame main = new MainFrame(services, account);
        main.setVisible(true);
    }

    /** Focuses the first field once the window is on screen. */
    public void focusFirstField() {
        handleField.requestFocusInWindow();
    }

    /** Pre-fills the sign-in handle, used to point a first-time reader at the demo account. */
    public void prefill(String handle, String password) {
        handleField.setText(handle);
        passwordField.setText(password);
    }

    /**
     * The left-hand branding panel: a gradient, the product name, and the headline features.
     */
    private static final class BrandPanel extends JPanel {

        private static final int PANEL_WIDTH = 380;

        BrandPanel() {
            setPreferredSize(new Dimension(PANEL_WIDTH, 0));
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            setBorder(Theme.padding(Theme.SPACE_6 + Theme.SPACE_3, Theme.SPACE_6));

            JLabel mark = new JLabel("Library Manager",
                    VectorIcon.of(VectorIcon.Glyph.LIBRARY, 26, Color.WHITE), SwingConstants.LEFT);
            mark.setFont(Theme.titleFont());
            mark.setForeground(Color.WHITE);
            mark.setIconTextGap(Theme.SPACE_3);

            JLabel tagline = new JLabel("<html>Circulation, catalogue and fines<br>"
                    + "in one desktop app.</html>");
            tagline.setFont(Theme.bodyFont().deriveFont(15f));
            tagline.setForeground(new Color(0xFFFFFF));

            add(mark);
            add(Box.createVerticalStrut(Theme.SPACE_4));
            add(tagline);
            add(Box.createVerticalStrut(Theme.SPACE_6));
            add(feature(VectorIcon.Glyph.CLOCK, "Due dates and fines",
                    "Overdue copies are flagged and fines add up automatically."));
            add(Box.createVerticalStrut(Theme.SPACE_4));
            add(feature(VectorIcon.Glyph.BOOK_OPEN, "Catalogue by ISBN",
                    "Type a barcode and the title, author and cover fill themselves in."));
            add(Box.createVerticalStrut(Theme.SPACE_4));
            add(feature(VectorIcon.Glyph.CIRCULATION, "Full borrowing history",
                    "Every loan is kept, so nothing disappears when a book comes back."));
            add(Box.createVerticalGlue());

            for (Component child : getComponents()) {
                if (child instanceof javax.swing.JComponent jc) {
                    jc.setAlignmentX(LEFT_ALIGNMENT);
                }
            }
        }

        private static JPanel feature(VectorIcon.Glyph glyph, String title, String detail) {
            JPanel row = new JPanel(new BorderLayout(Theme.SPACE_3, 0));
            row.setOpaque(false);
            row.setBorder(BorderFactory.createEmptyBorder());

            JLabel icon = new JLabel(VectorIcon.of(glyph, 20, new Color(0xFFFFFF)));
            icon.setVerticalAlignment(SwingConstants.TOP);

            JPanel text = new JPanel();
            text.setOpaque(false);
            text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
            JLabel titleLabel = new JLabel(title);
            titleLabel.setFont(Theme.bodyBoldFont());
            titleLabel.setForeground(Color.WHITE);
            JLabel detailLabel = new JLabel("<html>" + detail + "</html>");
            detailLabel.setFont(Theme.smallFont());
            detailLabel.setForeground(new Color(255, 255, 255, 205));
            titleLabel.setAlignmentX(LEFT_ALIGNMENT);
            detailLabel.setAlignmentX(LEFT_ALIGNMENT);
            text.add(titleLabel);
            text.add(Box.createVerticalStrut(2));
            text.add(detailLabel);

            row.add(icon, BorderLayout.WEST);
            row.add(text, BorderLayout.CENTER);
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, row.getPreferredSize().height));
            return row;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setPaint(new GradientPaint(0, 0, Theme.accent(),
                        getWidth(), getHeight(), Theme.accentPressed()));
                g2.fillRect(0, 0, getWidth(), getHeight());

                // A pair of soft circles to keep the flat gradient from looking bare.
                g2.setColor(new Color(255, 255, 255, 22));
                g2.fillOval(getWidth() - 120, -60, 260, 260);
                g2.fillOval(-90, getHeight() - 150, 220, 220);
            } finally {
                g2.dispose();
            }
        }
    }
}
