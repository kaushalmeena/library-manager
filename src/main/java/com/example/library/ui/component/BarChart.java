package com.example.library.ui.component;

import com.example.library.ui.theme.Theme;

import javax.swing.JPanel;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.util.List;

/**
 * A compact column chart for the dashboard's monthly loan activity.
 *
 * <p>Bars are scaled to the largest value in the series, the newest month is highlighted in the
 * accent colour, and each column is labelled with its month and value.
 */
public final class BarChart extends JPanel {

    private static final int BAR_WIDTH = 26;
    private static final int BAR_GAP = 14;
    private static final int LABEL_HEIGHT = 18;
    private static final int VALUE_HEIGHT = 16;
    private static final int MIN_PLOT_HEIGHT = 90;

    /**
     * One column of the chart.
     *
     * @param label the axis label, e.g. {@code Jul}
     * @param value the bar's height in data terms
     */
    public record Bar(String label, int value) {
    }

    private List<Bar> bars = List.of();

    public BarChart() {
        setOpaque(false);
    }

    /** Replaces the series and repaints. */
    public void setBars(List<Bar> newBars) {
        this.bars = newBars == null ? List.of() : List.copyOf(newBars);
        revalidate();
        repaint();
    }

    @Override
    public Dimension getPreferredSize() {
        int width = Math.max(1, bars.size()) * (BAR_WIDTH + BAR_GAP);
        return new Dimension(width, MIN_PLOT_HEIGHT + LABEL_HEIGHT + VALUE_HEIGHT);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (bars.isEmpty()) {
            return;
        }
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int max = bars.stream().mapToInt(Bar::value).max().orElse(0);
            int plotHeight = Math.max(MIN_PLOT_HEIGHT,
                    getHeight() - LABEL_HEIGHT - VALUE_HEIGHT);
            int baseline = VALUE_HEIGHT + plotHeight;

            // Spread the columns evenly across whatever width the layout gave us.
            int step = bars.size() == 0 ? 0 : getWidth() / bars.size();
            int barWidth = Math.min(BAR_WIDTH, Math.max(8, step - BAR_GAP));

            g2.setColor(Theme.divider());
            g2.drawLine(0, baseline + 1, getWidth(), baseline + 1);

            g2.setFont(Theme.smallFont());
            FontMetrics metrics = g2.getFontMetrics();

            for (int i = 0; i < bars.size(); i++) {
                Bar bar = bars.get(i);
                int centre = step * i + step / 2;
                int x = centre - barWidth / 2;
                // Keep an empty month visible as a sliver rather than nothing at all.
                int height = max == 0 ? 2 : Math.max(2, Math.round(bar.value() * plotHeight / (float) max));
                int y = baseline - height;

                boolean newest = i == bars.size() - 1;
                g2.setColor(newest ? Theme.accent() : Theme.tint(Theme.accent()));
                g2.fill(new RoundRectangle2D.Float(x, y, barWidth, height, 6, 6));

                if (bar.value() > 0) {
                    String value = Integer.toString(bar.value());
                    g2.setColor(newest ? Theme.textPrimary() : Theme.textSecondary());
                    g2.drawString(value, centre - metrics.stringWidth(value) / 2, y - 5);
                }

                g2.setColor(Theme.textMuted());
                g2.drawString(bar.label(),
                        centre - metrics.stringWidth(bar.label()) / 2,
                        baseline + LABEL_HEIGHT - 4);
            }
        } finally {
            g2.dispose();
        }
    }
}
