package com.example.library.ui.view;

import com.example.library.LibraryServices;
import com.example.library.model.LibraryStats;
import com.example.library.model.LoanDetail;
import com.example.library.model.RankedTitle;
import com.example.library.model.User;
import com.example.library.service.CirculationService;
import com.example.library.service.StatsService;
import com.example.library.ui.component.BarChart;
import com.example.library.ui.component.Badge;
import com.example.library.ui.component.Buttons;
import com.example.library.ui.component.Card;
import com.example.library.ui.component.StatCard;
import com.example.library.ui.support.Formats;
import com.example.library.ui.theme.Theme;
import com.example.library.ui.theme.VectorIcon;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.time.LocalDate;
import java.util.List;
import java.util.function.Consumer;

/**
 * The landing screen. Staff see the whole library at a glance; students see their own shelf.
 *
 * <p>The tiles, chart and lists are built once and repopulated on {@link #refresh()}, so moving
 * between screens does not rebuild the layout.
 */
public final class DashboardView extends View {

    private static final int ACTIVITY_MONTHS = 6;
    private static final int LEADERBOARD_SIZE = 4;

    /** How many rows the needs-attention list shows before it would need to scroll. */
    private static final int ATTENTION_ROWS = 4;

    private static final int TILE_HEIGHT = 112;
    private static final int CHART_HEIGHT = 216;

    private final LibraryServices services;
    private final User account;
    private final Consumer<String> navigate;

    private final StatCard firstCard;
    private final StatCard secondCard;
    private final StatCard thirdCard;
    private final StatCard fourthCard;
    private final BarChart activityChart = new BarChart();
    private final JPanel attentionList = new JPanel();
    private final JPanel leaderboardList = new JPanel();
    private final JLabel activityCaption = new JLabel();
    private final Card attentionCard;
    private final Card leaderboardCard;

    private final boolean staffView;

    public DashboardView(LibraryServices services, User account, Consumer<String> navigate) {
        super("Dashboard", "Loading…");
        this.services = services;
        this.account = account;
        this.navigate = navigate;
        this.staffView = account.role().canSeeAllLoans();

        if (staffView) {
            firstCard = new StatCard("Titles", VectorIcon.Glyph.BOOKS, Theme.accent());
            secondCard = new StatCard("On loan", VectorIcon.Glyph.CIRCULATION, Theme.info());
            thirdCard = new StatCard("Overdue", VectorIcon.Glyph.WARNING, Theme.danger());
            fourthCard = new StatCard("Fines owed", VectorIcon.Glyph.COIN, Theme.warning());
        } else {
            firstCard = new StatCard("Books held", VectorIcon.Glyph.BOOKS, Theme.accent());
            secondCard = new StatCard("Due soon", VectorIcon.Glyph.CLOCK, Theme.info());
            thirdCard = new StatCard("Overdue", VectorIcon.Glyph.WARNING, Theme.danger());
            fourthCard = new StatCard("Fines owed", VectorIcon.Glyph.COIN, Theme.warning());
        }

        attentionList.setOpaque(false);
        attentionList.setLayout(new BoxLayout(attentionList, BoxLayout.Y_AXIS));
        leaderboardList.setOpaque(false);
        leaderboardList.setLayout(new BoxLayout(leaderboardList, BoxLayout.Y_AXIS));

        activityCaption.setFont(Theme.smallFont());
        activityCaption.setForeground(Theme.textMuted());

        attentionCard = Card.titled(
                staffView ? "Needs attention" : "Your current loans",
                linkButton(staffView ? "See all loans" : "See my history", CIRCULATION_KEY),
                attentionList);

        leaderboardCard = Card.titled(
                staffView ? "Most borrowed" : "Popular in the library",
                linkButton("Browse catalogue", CATALOGUE_KEY),
                leaderboardList);

        setBody(buildBody());
        addAction(refreshButton());
    }

    private static final String CIRCULATION_KEY = "circulation";
    private static final String CATALOGUE_KEY = "catalogue";

    private JButton refreshButton() {
        JButton button = Buttons.secondary("Refresh", VectorIcon.Glyph.REFRESH);
        button.addActionListener(e -> refresh());
        return button;
    }

    private JButton linkButton(String text, String destination) {
        JButton button = new JButton(text,
                VectorIcon.of(VectorIcon.Glyph.CHEVRON_RIGHT, 13, Theme.accent()));
        button.setFont(Theme.smallBoldFont());
        button.setForeground(Theme.accent());
        button.setHorizontalTextPosition(SwingConstants.LEFT);
        button.setIconTextGap(Theme.SPACE_1);
        Theme.asQuietButton(button);
        button.addActionListener(e -> navigate.accept(destination));
        return button;
    }

    /**
     * Lays the dashboard out so it always fits the window.
     *
     * <p>The tiles and the chart keep fixed heights and the two lists absorb whatever is left,
     * which means the screen never needs a scrollbar — a dashboard you have to scroll is not
     * really an at-a-glance view.
     */
    private javax.swing.JComponent buildBody() {
        JPanel tiles = new JPanel(new GridLayout(1, 4, Theme.SPACE_4, 0));
        tiles.setOpaque(false);
        tiles.add(firstCard);
        tiles.add(secondCard);
        tiles.add(thirdCard);
        tiles.add(fourthCard);
        tiles.setPreferredSize(new Dimension(0, TILE_HEIGHT));

        JPanel chartBody = new JPanel(new BorderLayout(0, Theme.SPACE_2));
        chartBody.setOpaque(false);
        chartBody.add(activityChart, BorderLayout.CENTER);
        chartBody.add(activityCaption, BorderLayout.SOUTH);
        Card chartCard = Card.titled(
                staffView ? "Loans per month" : "Your borrowing", chartBody);
        chartCard.setPreferredSize(new Dimension(0, CHART_HEIGHT));

        JPanel lowerRow = new JPanel(new GridLayout(1, 2, Theme.SPACE_4, 0));
        lowerRow.setOpaque(false);
        lowerRow.add(attentionCard);
        lowerRow.add(leaderboardCard);

        JPanel middle = new JPanel(new BorderLayout(0, Theme.SPACE_4));
        middle.setOpaque(false);
        middle.add(chartCard, BorderLayout.NORTH);
        middle.add(lowerRow, BorderLayout.CENTER);

        JPanel root = new JPanel(new BorderLayout(0, Theme.SPACE_4));
        root.setOpaque(false);
        root.add(tiles, BorderLayout.NORTH);
        root.add(middle, BorderLayout.CENTER);
        return root;
    }

    @Override
    public void refresh() {
        if (staffView) {
            refreshStaffView();
        } else {
            refreshMemberView();
        }
        refreshActivityChart();
    }

    private void refreshStaffView() {
        LibraryStats stats = services.statsService().libraryStats();
        LocalDate today = services.circulationService().today();

        setSubtitle("Good to see you, " + firstName() + ". Here is the library on "
                + Formats.date(today) + ".");

        firstCard.setValue(Integer.toString(stats.titles()))
                .setCaption(Formats.plural(stats.copies(), "copy", "copies") + " in total");
        secondCard.setValue(Integer.toString(stats.onLoan()))
                .setCaption(stats.utilisationPercent() + "% of the collection");
        thirdCard.setValue(Integer.toString(stats.overdue()))
                .setCaption(stats.dueSoon() + " more due soon");
        fourthCard.setValue(services.config().money(stats.outstandingFines()))
                .setCaption("across " + Formats.plural(stats.onLoan(), "open loan", "open loans"));

        List<LoanDetail> overdue = services.circulationService().overdueLoans();
        List<LoanDetail> dueSoon = services.circulationService().loansDueSoon();
        attentionList.removeAll();
        if (overdue.isEmpty() && dueSoon.isEmpty()) {
            attentionList.add(emptyState("Nothing is overdue or due soon. All caught up."));
        } else {
            overdue.stream().limit(ATTENTION_ROWS).forEach(detail ->
                    attentionList.add(loanRow(detail, today)));
            dueSoon.stream().limit(Math.max(0, ATTENTION_ROWS - overdue.size()))
                    .forEach(detail -> attentionList.add(loanRow(detail, today)));
        }
        attentionList.revalidate();
        attentionList.repaint();

        populateLeaderboard(services.statsService().mostBorrowedTitles(LEADERBOARD_SIZE),
                "No loans recorded yet.");
    }

    private void refreshMemberView() {
        StatsService.MemberStats stats = services.statsService().memberStats(account.id());
        LocalDate today = services.circulationService().today();
        List<LoanDetail> mine = services.circulationService().outstandingLoansForUser(account.id());

        long dueSoon = mine.stream()
                .filter(detail -> !detail.loan().isOverdueOn(today))
                .filter(detail -> detail.loan().daysUntilDue(today)
                        <= CirculationService.DUE_SOON_WINDOW_DAYS)
                .count();

        setSubtitle("Welcome back, " + firstName() + ". You have borrowed "
                + Formats.plural(stats.borrowedEverTotal(), "book", "books") + " in total.");

        firstCard.setValue(Integer.toString(stats.currentlyHeld()))
                .setCaption("limit is " + services.config().maxLoansPerMember() + " at a time");
        secondCard.setValue(Long.toString(dueSoon))
                .setCaption("in the next 3 days");
        thirdCard.setValue(Integer.toString(stats.overdue()))
                .setCaption(stats.overdue() == 0
                        ? "nothing late"
                        : "return them to borrow");
        fourthCard.setValue(services.config().money(stats.finesOwed()))
                .setCaption(services.config().money(services.config().finePerDay()) + " per late day");

        attentionList.removeAll();
        if (mine.isEmpty()) {
            attentionList.add(emptyState("You are not holding any books right now."));
        } else {
            mine.stream().limit(ATTENTION_ROWS)
                    .forEach(detail -> attentionList.add(loanRow(detail, today)));
        }
        attentionList.revalidate();
        attentionList.repaint();

        populateLeaderboard(services.statsService().mostBorrowedTitles(LEADERBOARD_SIZE),
                "No loans recorded yet.");
    }

    private void refreshActivityChart() {
        // Library-wide volume is meaningless on a personal dashboard, so a member sees their own.
        List<StatsService.MonthlyActivity> activity = staffView
                ? services.statsService().monthlyActivity(ACTIVITY_MONTHS)
                : services.statsService().monthlyActivityForUser(account.id(), ACTIVITY_MONTHS);
        activityChart.setBars(activity.stream()
                .map(month -> new BarChart.Bar(month.shortLabel(), month.loans()))
                .toList());
        int total = activity.stream().mapToInt(StatsService.MonthlyActivity::loans).sum();
        activityCaption.setText((staffView ? "" : "you borrowed ")
                + Formats.plural(total, "loan", "loans") + " over the last "
                + ACTIVITY_MONTHS + " months");
    }

    private void populateLeaderboard(List<RankedTitle> ranked, String emptyMessage) {
        leaderboardList.removeAll();
        if (ranked.isEmpty()) {
            leaderboardList.add(emptyState(emptyMessage));
        } else {
            int position = 1;
            for (RankedTitle entry : ranked) {
                leaderboardList.add(rankedRow(position++, entry));
            }
        }
        leaderboardList.revalidate();
        leaderboardList.repaint();
    }

    /** One line in the "needs attention" list: title, who has it, and a status badge. */
    private JPanel loanRow(LoanDetail detail, LocalDate today) {
        JPanel row = new JPanel(new BorderLayout(Theme.SPACE_3, 0));
        row.setOpaque(false);
        row.setBorder(Theme.padding(Theme.SPACE_2, 0));

        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));

        JLabel title = new JLabel(detail.bookTitle());
        title.setFont(Theme.bodyBoldFont());
        title.setForeground(Theme.textPrimary());

        String secondary = staffView
                ? detail.memberName() + " · "
                        + Formats.relativeDueDate(detail.loan().dueDate(), today)
                : Formats.relativeDueDate(detail.loan().dueDate(), today)
                        + " · due " + Formats.date(detail.loan().dueDate());
        JLabel meta = new JLabel(secondary);
        meta.setFont(Theme.smallFont());
        meta.setForeground(Theme.textMuted());

        title.setAlignmentX(LEFT_ALIGNMENT);
        meta.setAlignmentX(LEFT_ALIGNMENT);
        text.add(title);
        text.add(meta);

        Badge badge = Badge.forStatus(
                detail.status(today, CirculationService.DUE_SOON_WINDOW_DAYS));
        JPanel badgeHolder = new JPanel(new BorderLayout());
        badgeHolder.setOpaque(false);
        badgeHolder.add(badge, BorderLayout.CENTER);

        row.add(text, BorderLayout.CENTER);
        row.add(badgeHolder, BorderLayout.EAST);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, row.getPreferredSize().height));
        row.setAlignmentX(LEFT_ALIGNMENT);
        return row;
    }

    /** One line in the leaderboard: position, title, author, and a loan count. */
    private JPanel rankedRow(int position, RankedTitle entry) {
        JPanel row = new JPanel(new BorderLayout(Theme.SPACE_3, 0));
        row.setOpaque(false);
        row.setBorder(Theme.padding(Theme.SPACE_2, 0));

        JLabel rank = new JLabel(Integer.toString(position));
        rank.setFont(Theme.bodyBoldFont());
        rank.setForeground(Theme.textMuted());
        rank.setPreferredSize(new Dimension(16, 16));

        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        JLabel label = new JLabel(entry.label());
        label.setFont(Theme.bodyFont());
        label.setForeground(Theme.textPrimary());
        JLabel sublabel = new JLabel(entry.sublabel() == null ? " " : entry.sublabel());
        sublabel.setFont(Theme.smallFont());
        sublabel.setForeground(Theme.textMuted());
        label.setAlignmentX(LEFT_ALIGNMENT);
        sublabel.setAlignmentX(LEFT_ALIGNMENT);
        text.add(label);
        text.add(sublabel);

        Badge count = Badge.neutral(Formats.plural(entry.count(), "loan", "loans"));

        row.add(rank, BorderLayout.WEST);
        row.add(text, BorderLayout.CENTER);
        row.add(count, BorderLayout.EAST);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, row.getPreferredSize().height));
        row.setAlignmentX(LEFT_ALIGNMENT);
        return row;
    }

    private Component emptyState(String message) {
        JLabel label = new JLabel(message);
        label.setFont(Theme.bodyFont());
        label.setForeground(Theme.textMuted());
        label.setBorder(Theme.padding(Theme.SPACE_4, 0));
        label.setAlignmentX(LEFT_ALIGNMENT);
        return label;
    }

    private String firstName() {
        String[] parts = account.name().trim().split("\\s+");
        return parts.length == 0 ? account.name() : parts[0];
    }
}
