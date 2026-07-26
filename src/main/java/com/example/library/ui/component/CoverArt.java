package com.example.library.ui.component;

import com.example.library.ui.support.Async;
import com.example.library.ui.theme.Theme;
import com.example.library.ui.theme.VectorIcon;

import javax.imageio.ImageIO;
import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Shows a book's cover, downloading it in the background and falling back to a drawn
 * placeholder when there is no artwork.
 *
 * <p>Downloads are cached for the life of the process, so scrolling a table or reopening the
 * edit dialog does not refetch the same image. The cache is bounded so a long session cannot
 * grow it without limit.
 */
public final class CoverArt extends JPanel {

    private static final int MAX_CACHE_ENTRIES = 200;
    private static final int CONNECT_TIMEOUT_MS = 8000;
    private static final int READ_TIMEOUT_MS = 8000;

    /** Least-recently-used cache of already downloaded covers, keyed by URL. */
    private static final Map<String, BufferedImage> CACHE = Collections.synchronizedMap(
            new LinkedHashMap<>(32, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, BufferedImage> eldest) {
                    return size() > MAX_CACHE_ENTRIES;
                }
            });

    private final int radius;

    private BufferedImage image;
    private String currentUrl;
    private boolean loading;

    public CoverArt(int width, int height) {
        this.radius = 8;
        setOpaque(false);
        setPreferredSize(new Dimension(width, height));
        setMinimumSize(new Dimension(width, height));
    }

    /**
     * Points the component at a cover URL. Passing {@code null} clears it back to the
     * placeholder. Safe to call repeatedly; a URL already on screen is ignored.
     */
    public void setCoverUrl(String url) {
        if (Objects.equals(currentUrl, url)) {
            return;
        }
        currentUrl = url;
        image = null;
        loading = false;

        if (url == null || url.isBlank()) {
            repaint();
            return;
        }
        BufferedImage cached = CACHE.get(url);
        if (cached != null) {
            image = cached;
            repaint();
            return;
        }
        loading = true;
        repaint();

        Async.run(() -> download(url), downloaded -> {
            // A later call may have moved on to a different cover while this one was in flight.
            if (!Objects.equals(currentUrl, url)) {
                return;
            }
            loading = false;
            if (downloaded != null) {
                CACHE.put(url, downloaded);
                image = downloaded;
            }
            repaint();
        }, error -> {
            if (Objects.equals(currentUrl, url)) {
                loading = false;
                repaint();
            }
        });
    }

    /** Downloads and decodes a cover, returning {@code null} when unavailable. */
    private static BufferedImage download(String url) {
        try {
            HttpURLConnection connection =
                    (HttpURLConnection) URI.create(url).toURL().openConnection();
            connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
            connection.setReadTimeout(READ_TIMEOUT_MS);
            connection.setRequestProperty("User-Agent", "library-manager/2.0");
            try (InputStream in = connection.getInputStream()) {
                BufferedImage decoded = ImageIO.read(in);
                // Open Library answers a missing cover with a 1x1 placeholder pixel.
                if (decoded == null || decoded.getWidth() <= 2 || decoded.getHeight() <= 2) {
                    return null;
                }
                return decoded;
            }
        } catch (IOException | IllegalArgumentException e) {
            return null;
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR);

            int width = getWidth();
            int height = getHeight();
            RoundRectangle2D shape =
                    new RoundRectangle2D.Float(0, 0, width, height, radius, radius);

            if (image != null) {
                // A plain background behind the artwork, since fitting can leave margins.
                g2.setColor(Theme.surface());
                g2.fill(shape);
                g2.setClip(shape);
                drawScaledToFit(g2, image, width, height);
                g2.setClip(null);
                g2.setColor(Theme.border());
                g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, width - 1f, height - 1f,
                        radius, radius));
            } else {
                g2.setColor(Theme.accentSoft());
                g2.fill(shape);
                g2.setColor(Theme.border());
                g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, width - 1f, height - 1f,
                        radius, radius));
                paintPlaceholder(g2, width, height);
            }
        } finally {
            g2.dispose();
        }
    }

    /**
     * Scales the artwork to sit entirely inside the frame, centred, keeping its aspect ratio.
     *
     * <p>Cropping to fill would look tidier but cuts the edges off a book cover, which is where
     * the title usually is, so the whole image is shown even if that leaves a margin.
     */
    private static void drawScaledToFit(Graphics2D g2, BufferedImage source, int width,
                                        int height) {
        double scale = Math.min(width / (double) source.getWidth(),
                height / (double) source.getHeight());
        int scaledWidth = Math.max(1, (int) Math.round(source.getWidth() * scale));
        int scaledHeight = Math.max(1, (int) Math.round(source.getHeight() * scale));
        int x = (width - scaledWidth) / 2;
        int y = (height - scaledHeight) / 2;
        g2.drawImage(source, x, y, scaledWidth, scaledHeight, null);
    }

    private void paintPlaceholder(Graphics2D g2, int width, int height) {
        int iconSize = Math.min(32, Math.min(width, height) / 2);
        Color iconColor = loading ? Theme.textMuted() : Theme.accent();
        VectorIcon icon = VectorIcon.of(
                loading ? VectorIcon.Glyph.CLOCK : VectorIcon.Glyph.BOOK_OPEN,
                iconSize, iconColor);
        icon.paintIcon(this, g2, (width - iconSize) / 2, (height - iconSize) / 2);
    }

    /** A cover thumbnail scaled for a table cell, or {@code null} when not yet downloaded. */
    public static Image cachedThumbnail(String url, int width, int height) {
        if (url == null) {
            return null;
        }
        BufferedImage cached = CACHE.get(url);
        return cached == null ? null : cached.getScaledInstance(width, height, Image.SCALE_SMOOTH);
    }
}
