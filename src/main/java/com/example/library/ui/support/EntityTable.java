package com.example.library.ui.support;

import com.example.library.model.LoanStatus;
import com.example.library.model.Role;
import com.example.library.ui.component.Badge;
import com.example.library.ui.theme.Theme;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.RowSorter;
import javax.swing.ScrollPaneConstants;
import javax.swing.SortOrder;
import javax.swing.SwingConstants;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import javax.swing.table.TableRowSorter;
import javax.swing.table.TableStringConverter;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * A sortable, searchable table over a list of domain objects.
 *
 * <p>Views describe their columns and hand over rows; this class owns the model, the styling,
 * the search filter and the selection plumbing. Because rows are the domain objects themselves,
 * acting on a selection never involves parsing text back out of a cell, which is what the old
 * "type the book id" dialogs had to do.
 *
 * @param <T> the row type
 */
public final class EntityTable<T> extends JPanel {

    /** Horizontal placement of a column's contents. */
    public enum Align { LEFT, CENTER, RIGHT }

    /**
     * One column.
     *
     * @param <T> the row type
     */
    public static final class Column<T> {

        private final String title;
        private final int preferredWidth;
        private final Function<T, Object> value;
        private final Function<Object, String> display;
        private final Align align;
        private final boolean emphasised;

        private Column(String title, int preferredWidth, Function<T, Object> value,
                       Function<Object, String> display, Align align, boolean emphasised) {
            this.title = title;
            this.preferredWidth = preferredWidth;
            this.value = value;
            this.display = display;
            this.align = align;
            this.emphasised = emphasised;
        }

        /** A text column. */
        public static <T> Column<T> of(String title, int width, Function<T, Object> value) {
            return new Column<>(title, width, value, Column::defaultDisplay, Align.LEFT, false);
        }

        /** A column whose value needs custom formatting, e.g. a date or a money amount. */
        public static <T> Column<T> of(String title, int width, Function<T, Object> value,
                                       Function<Object, String> display) {
            return new Column<>(title, width, value, display, Align.LEFT, false);
        }

        /** Right-aligns the column, which suits numbers. */
        public Column<T> alignRight() {
            return new Column<>(title, preferredWidth, value, display, Align.RIGHT, emphasised);
        }

        /** Centres the column, which suits badges. */
        public Column<T> alignCenter() {
            return new Column<>(title, preferredWidth, value, display, Align.CENTER, emphasised);
        }

        /** Renders the column in bold, for the row's primary identifier. */
        public Column<T> emphasised() {
            return new Column<>(title, preferredWidth, value, display, align, true);
        }

        private static String defaultDisplay(Object value) {
            if (value == null) {
                return "";
            }
            if (value instanceof LoanStatus status) {
                return status.label();
            }
            if (value instanceof Role role) {
                return role.displayName();
            }
            return String.valueOf(value);
        }

        String title() {
            return title;
        }

        String textFor(T row) {
            return display.apply(value.apply(row));
        }
    }

    private final List<Column<T>> columns;
    private final RowModel model;
    private final JTable table;
    private final TableRowSorter<RowModel> sorter;
    private final JLabel emptyLabel;
    private final JScrollPane scrollPane;

    private List<T> rows = List.of();
    private String emptyMessage = "Nothing to show yet.";
    private String filterText;
    private java.util.function.Predicate<T> rowPredicate;

    public EntityTable(List<Column<T>> columns) {
        super(new BorderLayout());
        this.columns = List.copyOf(columns);
        this.model = new RowModel();
        this.table = new JTable(model);
        this.sorter = new TableRowSorter<>(model);

        setOpaque(false);
        configureTable();

        scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setOpaque(false);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        emptyLabel = new JLabel(emptyMessage, SwingConstants.CENTER);
        emptyLabel.setFont(Theme.bodyFont());
        emptyLabel.setForeground(Theme.textMuted());
        emptyLabel.setBorder(Theme.padding(Theme.SPACE_6, Theme.SPACE_4));
        emptyLabel.setVisible(false);

        add(scrollPane, BorderLayout.CENTER);
        add(emptyLabel, BorderLayout.SOUTH);
    }

    private void configureTable() {
        table.setRowSorter(sorter);
        table.setAutoCreateRowSorter(false);
        table.setFillsViewportHeight(true);
        table.setShowGrid(false);
        table.setShowHorizontalLines(true);
        table.setIntercellSpacing(new Dimension(0, 1));
        table.setRowHeight(Theme.TABLE_ROW_HEIGHT);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        table.setFont(Theme.bodyFont());
        table.setOpaque(false);
        table.putClientProperty("JTable.rowSelectionBackground", Theme.accentSoft());

        JTableHeader header = table.getTableHeader();
        header.setFont(Theme.smallBoldFont());
        header.setReorderingAllowed(false);
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.border()));

        for (int i = 0; i < columns.size(); i++) {
            Column<T> column = columns.get(i);
            TableColumn tableColumn = table.getColumnModel().getColumn(i);
            tableColumn.setPreferredWidth(column.preferredWidth);
            tableColumn.setCellRenderer(new CellRenderer(column));
            // A header centred over a right-aligned column reads as a different column.
            tableColumn.setHeaderRenderer(new HeaderRenderer(column, header.getDefaultRenderer()));
        }

        // Search should match what is on screen, not an object's toString.
        sorter.setStringConverter(new TableStringConverter() {
            @Override
            public String toString(javax.swing.table.TableModel ignored, int row, int column) {
                return columns.get(column).textFor(rows.get(row));
            }
        });

        // Every column holds plain Objects, so without this dates and numbers would sort as
        // text and "10" would land before "2".
        for (int i = 0; i < columns.size(); i++) {
            sorter.setComparator(i, NATURAL_ORDER);
        }
    }

    /**
     * Sorts values by their natural order when they are comparable and of the same type, and by
     * their printed form otherwise. Nulls sort first.
     */
    private static final Comparator<Object> NATURAL_ORDER = (left, right) -> {
        if (left == null || right == null) {
            return left == right ? 0 : (left == null ? -1 : 1);
        }
        if (left instanceof Comparable<?> && left.getClass() == right.getClass()) {
            @SuppressWarnings("unchecked")
            Comparable<Object> comparable = (Comparable<Object>) left;
            return comparable.compareTo(right);
        }
        return String.valueOf(left).compareToIgnoreCase(String.valueOf(right));
    };

    /** Replaces the rows, keeping the current sort order and search filter. */
    public void setRows(List<T> newRows) {
        this.rows = newRows == null ? List.of() : List.copyOf(newRows);
        model.fireTableDataChanged();
        updateEmptyState();
    }

    /** The rows currently held, unfiltered and in insertion order. */
    public List<T> rows() {
        return rows;
    }

    /** The selected row, or empty when nothing is selected. */
    public Optional<T> selectedRow() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) {
            return Optional.empty();
        }
        int modelRow = table.convertRowIndexToModel(viewRow);
        return modelRow >= 0 && modelRow < rows.size()
                ? Optional.of(rows.get(modelRow))
                : Optional.empty();
    }

    /** Selects the row equal to {@code row}, if it is present. */
    public void selectRow(T row) {
        int index = rows.indexOf(row);
        if (index < 0) {
            return;
        }
        int viewRow = table.convertRowIndexToView(index);
        if (viewRow >= 0) {
            table.setRowSelectionInterval(viewRow, viewRow);
            table.scrollRectToVisible(table.getCellRect(viewRow, 0, true));
        }
    }

    /**
     * Narrows the visible rows to those containing {@code text} in any column.
     *
     * <p>The text is quoted so a search for something like "C++" is taken literally rather than
     * as a regular expression.
     */
    public void setFilterText(String text) {
        this.filterText = text == null || text.isBlank() ? null : text.trim();
        applyFilters();
    }

    /**
     * Restricts the rows to those matching {@code predicate}, which is combined with, rather
     * than replacing, the search text. Pass {@code null} to clear it.
     */
    public void setRowPredicate(java.util.function.Predicate<T> predicate) {
        this.rowPredicate = predicate;
        applyFilters();
    }

    /** Rebuilds the sorter's filter from the search text and the row predicate together. */
    private void applyFilters() {
        RowFilter<RowModel, Integer> textFilter = filterText == null
                ? null
                : RowFilter.regexFilter("(?i)" + Pattern.quote(filterText));
        RowFilter<RowModel, Integer> predicateFilter = rowPredicate == null ? null
                : new RowFilter<>() {
                    @Override
                    public boolean include(Entry<? extends RowModel, ? extends Integer> entry) {
                        int index = entry.getIdentifier();
                        return index >= 0 && index < rows.size()
                                && rowPredicate.test(rows.get(index));
                    }
                };

        if (textFilter == null && predicateFilter == null) {
            sorter.setRowFilter(null);
        } else if (textFilter == null) {
            sorter.setRowFilter(predicateFilter);
        } else if (predicateFilter == null) {
            sorter.setRowFilter(textFilter);
        } else {
            sorter.setRowFilter(RowFilter.andFilter(List.of(textFilter, predicateFilter)));
        }
        updateEmptyState();
    }

    /** Sorts by a column index. */
    public void sortBy(int columnIndex, boolean ascending) {
        sorter.setSortKeys(List.of(new RowSorter.SortKey(columnIndex,
                ascending ? SortOrder.ASCENDING : SortOrder.DESCENDING)));
    }

    /** Calls {@code handler} when a row is double-clicked. */
    public void onRowActivated(Consumer<T> handler) {
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && e.getButton() == MouseEvent.BUTTON1) {
                    selectedRow().ifPresent(handler);
                }
            }
        });
    }

    /** Calls {@code handler} whenever the selection changes. */
    public void onSelectionChanged(Consumer<Optional<T>> handler) {
        table.getSelectionModel().addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) {
                handler.accept(selectedRow());
            }
        });
    }

    /** The message shown when there are no rows to display. */
    public void setEmptyMessage(String message) {
        this.emptyMessage = message;
        emptyLabel.setText(message);
        updateEmptyState();
    }

    /** How many rows the current filter leaves visible. */
    public int visibleRowCount() {
        return table.getRowCount();
    }

    public JTable table() {
        return table;
    }

    /** Column headers, in order, for a CSV export. */
    public List<String> headers() {
        return columns.stream().map(Column::title).toList();
    }

    /** The visible rows in the order they appear, as text, for a CSV export. */
    public List<List<String>> visibleRowsAsText() {
        List<List<String>> out = new ArrayList<>(table.getRowCount());
        for (int viewRow = 0; viewRow < table.getRowCount(); viewRow++) {
            int modelRow = table.convertRowIndexToModel(viewRow);
            T row = rows.get(modelRow);
            List<String> cells = new ArrayList<>(columns.size());
            for (Column<T> column : columns) {
                cells.add(column.textFor(row));
            }
            out.add(cells);
        }
        return out;
    }
    private void updateEmptyState() {
        emptyLabel.setVisible(table.getRowCount() == 0);
        emptyLabel.setText(rows.isEmpty() ? emptyMessage : "No rows match your search.");
    }

    /** Bridges the row list to Swing's table model contract. */
    private final class RowModel extends AbstractTableModel {

        @Override
        public int getRowCount() {
            return rows.size();
        }

        @Override
        public int getColumnCount() {
            return columns.size();
        }

        @Override
        public String getColumnName(int column) {
            return columns.get(column).title;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            return columns.get(columnIndex).value.apply(rows.get(rowIndex));
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return false;
        }
    }

    /**
     * Draws a column header aligned the same way as the cells beneath it, delegating the rest of
     * the painting — the sort arrow and the platform styling — to the look and feel's renderer.
     */
    private final class HeaderRenderer implements javax.swing.table.TableCellRenderer {

        private final Column<T> column;
        private final javax.swing.table.TableCellRenderer delegate;

        HeaderRenderer(Column<T> column, javax.swing.table.TableCellRenderer delegate) {
            this.column = column;
            this.delegate = delegate;
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus,
                                                       int row, int col) {
            Component rendered = delegate.getTableCellRendererComponent(table, value, isSelected,
                    hasFocus, row, col);
            if (rendered instanceof JLabel label) {
                label.setHorizontalAlignment(switch (column.align) {
                    case LEFT -> SwingConstants.LEFT;
                    case CENTER -> SwingConstants.CENTER;
                    case RIGHT -> SwingConstants.RIGHT;
                });
                label.setFont(Theme.smallBoldFont());
                label.setForeground(Theme.textMuted());
                label.setBorder(Theme.padding(0, Theme.SPACE_3));
            }
            return rendered;
        }
    }

    /** Draws a cell: a tinted pill for statuses and roles, styled text for everything else. */
    private final class CellRenderer extends DefaultTableCellRenderer {

        private final Column<T> column;
        private final Badge badge = new Badge("", Theme.textSecondary());
        private final JPanel badgeHolder = new JPanel(new java.awt.GridBagLayout());

        CellRenderer(Column<T> column) {
            this.column = column;
            badgeHolder.setOpaque(true);
            badgeHolder.add(badge);
            setBorder(Theme.padding(0, Theme.SPACE_3));
            setHorizontalAlignment(switch (column.align) {
                case LEFT -> SwingConstants.LEFT;
                case CENTER -> SwingConstants.CENTER;
                case RIGHT -> SwingConstants.RIGHT;
            });
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus,
                                                       int row, int col) {
            if (value instanceof LoanStatus status) {
                return badgeCell(table, status.label(), Theme.statusColor(status), isSelected);
            }
            if (value instanceof Role role) {
                return badgeCell(table, role.displayName(), roleColour(role), isSelected);
            }

            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
            setText(column.display.apply(value));
            setFont(column.emphasised ? Theme.bodyBoldFont() : Theme.bodyFont());
            if (!isSelected) {
                setBackground(Theme.surface());
                setForeground(column.emphasised ? Theme.textPrimary() : Theme.textSecondary());
            } else {
                setBackground(Theme.accentSoft());
                setForeground(Theme.textPrimary());
            }
            return this;
        }

        private Component badgeCell(JTable table, String text, Color tone, boolean isSelected) {
            badge.setText(text);
            badge.setTone(tone);
            badge.setFont(Theme.smallBoldFont());
            badgeHolder.setBackground(isSelected ? Theme.accentSoft() : Theme.surface());
            return badgeHolder;
        }

        private Color roleColour(Role role) {
            return switch (role) {
                case ADMIN -> Theme.accent();
                case LIBRARIAN -> Theme.info();
                case STUDENT -> Theme.textSecondary();
            };
        }
    }
}
