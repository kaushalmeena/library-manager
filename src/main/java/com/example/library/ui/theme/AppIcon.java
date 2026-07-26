package com.example.library.ui.theme;

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.List;

/**
 * The window and dock icon, drawn to match {@code assets/logo.svg}.
 *
 * <p>Swing cannot load an SVG without an extra dependency, so the same mark is painted with
 * Java2D at several sizes and handed to the window manager, which picks whichever it needs.
 */
public final class AppIcon {

    /** The sizes a desktop environment is likely to ask for. */
    private static final int[] SIZES = {16, 20, 24, 32, 48, 64, 128, 256};

    /** The mark is described on this grid, matching the SVG's viewBox. */
    private static final float GRID = 96f;

    private static List<BufferedImage> cached;

    private AppIcon() {
    }

    /** The icon rendered at every size, for {@link java.awt.Window#setIconImages(List)}. */
    public static synchronized List<BufferedImage> images() {
        if (cached == null) {
            cached = java.util.Arrays.stream(SIZES).mapToObj(AppIcon::render).toList();
        }
        return cached;
    }

    private static BufferedImage render(int size) {
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = image.createGraphics();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL,
                    RenderingHints.VALUE_STROKE_PURE);
            float scale = size / GRID;
            g2.transform(AffineTransform.getScaleInstance(scale, scale));
            paintMark(g2);
        } finally {
            g2.dispose();
        }
        return image;
    }

    private static void paintMark(Graphics2D g2) {
        g2.setPaint(new GradientPaint(0, 0, new Color(0x6366F1), GRID, GRID, new Color(0x4338CA)));
        g2.fill(new RoundRectangle2D.Float(0, 0, GRID, GRID, 44, 44));

        // Three spines on a shelf, the same arrangement as the SVG.
        g2.setColor(new Color(255, 255, 255, 235));
        g2.fill(new RoundRectangle2D.Float(24, 38, 14, 34, 7, 7));

        g2.setColor(Color.WHITE);
        g2.fill(new RoundRectangle2D.Float(42, 27, 14, 45, 7, 7));

        Graphics2D tilted = (Graphics2D) g2.create();
        try {
            tilted.rotate(Math.toRadians(9), 67, 72);
            tilted.setColor(new Color(255, 255, 255, 209));
            tilted.fill(new RoundRectangle2D.Float(60, 42, 14, 30, 7, 7));
        } finally {
            tilted.dispose();
        }

        g2.setColor(new Color(255, 255, 255, 242));
        g2.fill(new RoundRectangle2D.Float(19, 74, 58, 5, 5, 5));
    }
}
