package com.example.library.ui.theme;

import com.github.weisj.jsvg.SVGDocument;
import com.github.weisj.jsvg.parser.LoaderContext;
import com.github.weisj.jsvg.parser.SVGLoader;
import com.github.weisj.jsvg.view.ViewBox;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Taskbar;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The window and dock icon, rasterised from {@code assets/logo.svg}.
 *
 * <p>The artwork is authored once, as the SVG the README displays, and rendered here at the
 * sizes a desktop environment asks for. Redrawing the mark in Java2D would mean maintaining it
 * twice and letting the two versions drift apart.
 *
 * <p>The file reaches the classpath as {@code /icons/logo.svg}; see the resource mapping in
 * {@code pom.xml}.
 */
public final class AppIcon {

    private static final Logger LOG = Logger.getLogger(AppIcon.class.getName());

    private static final String RESOURCE = "/icons/logo.svg";

    /** The sizes a desktop environment is likely to ask for. */
    private static final int[] SIZES = {16, 20, 24, 32, 48, 64, 128, 256};

    private static List<BufferedImage> cached;

    private AppIcon() {
    }

    /** The icon rendered at every size, for {@link java.awt.Window#setIconImages(List)}. */
    public static synchronized List<BufferedImage> images() {
        if (cached == null) {
            cached = render();
        }
        return cached;
    }

    /**
     * Sets the icon shown in the dock or taskbar.
     *
     * <p>{@code setIconImages} on a window is not enough on macOS, where the dock tile comes
     * from the application rather than the window and otherwise falls back to the generic Java
     * mascot. Everything here is optional, so an unsupported platform simply keeps its default.
     */
    public static void applyToTaskbar() {
        if (!Taskbar.isTaskbarSupported()) {
            return;
        }
        List<BufferedImage> images = images();
        if (images.isEmpty()) {
            return;
        }
        try {
            Taskbar taskbar = Taskbar.getTaskbar();
            if (taskbar.isSupported(Taskbar.Feature.ICON_IMAGE)) {
                // The largest render; the platform scales it down for the tile it needs.
                taskbar.setIconImage(images.get(images.size() - 1));
            }
        } catch (UnsupportedOperationException | SecurityException e) {
            LOG.log(Level.FINE, "The platform would not accept a dock icon", e);
        }
    }

    private static List<BufferedImage> render() {
        SVGDocument document = load();
        if (document == null) {
            // An icon is not worth failing startup over; the platform default will be used.
            return List.of();
        }
        List<BufferedImage> images = new ArrayList<>(SIZES.length);
        for (int size : SIZES) {
            images.add(rasterise(document, size));
        }
        return List.copyOf(images);
    }

    private static SVGDocument load() {
        try (InputStream in = AppIcon.class.getResourceAsStream(RESOURCE)) {
            if (in == null) {
                LOG.warning(() -> "Missing icon resource " + RESOURCE);
                return null;
            }
            return new SVGLoader().load(in, URI.create("classpath:" + RESOURCE),
                    LoaderContext.createDefault());
        } catch (IOException | RuntimeException e) {
            LOG.log(Level.WARNING, "Could not read the application icon", e);
            return null;
        }
    }

    private static BufferedImage rasterise(SVGDocument document, int size) {
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = image.createGraphics();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL,
                    RenderingHints.VALUE_STROKE_PURE);
            document.render(null, g2, new ViewBox(0, 0, size, size));
        } finally {
            g2.dispose();
        }
        return image;
    }
}
