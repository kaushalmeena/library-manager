package com.example.library.ui.theme;

import javax.swing.Icon;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;

/**
 * The application's icon set, drawn with Java2D rather than shipped as images.
 *
 * <p>Every glyph is described on a 24x24 grid and scaled to the requested size, so icons stay
 * crisp on any display and pick up the current theme colour without needing light and dark
 * copies of each asset.
 */
public final class VectorIcon implements Icon {

    /** The available glyphs. */
    public enum Glyph {
        DASHBOARD, BOOKS, CIRCULATION, MEMBERS, PROFILE, LOGOUT, SEARCH, PLUS, EDIT, TRASH,
        REFRESH, DOWNLOAD, SUN, MOON, CHECK, CLOCK, WARNING, RENEW, COIN, BOOK_OPEN, LIBRARY,
        CHEVRON_RIGHT, USER_CHECK
    }

    private static final float GRID = 24f;

    private final Glyph glyph;
    private final int size;
    private final Color color;
    private final float strokeWidth;

    private VectorIcon(Glyph glyph, int size, Color color, float strokeWidth) {
        this.glyph = glyph;
        this.size = size;
        this.color = color;
        this.strokeWidth = strokeWidth;
    }

    /** An icon in the given colour at the given pixel size. */
    public static VectorIcon of(Glyph glyph, int size, Color color) {
        return new VectorIcon(glyph, size, color, 1.7f);
    }

    /** An icon with a heavier stroke, for use at small sizes. */
    public static VectorIcon bold(Glyph glyph, int size, Color color) {
        return new VectorIcon(glyph, size, color, 2.1f);
    }

    @Override
    public int getIconWidth() {
        return size;
    }

    @Override
    public int getIconHeight() {
        return size;
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL,
                    RenderingHints.VALUE_STROKE_PURE);
            g2.translate(x, y);
            float scale = size / GRID;
            g2.transform(AffineTransform.getScaleInstance(scale, scale));
            g2.setColor(color);
            g2.setStroke(new BasicStroke(strokeWidth, BasicStroke.CAP_ROUND,
                    BasicStroke.JOIN_ROUND));
            draw(g2);
        } finally {
            g2.dispose();
        }
    }

    private void draw(Graphics2D g2) {
        switch (glyph) {
            case DASHBOARD -> {
                g2.draw(new RoundRectangle2D.Float(3, 3, 7.5f, 7.5f, 2, 2));
                g2.draw(new RoundRectangle2D.Float(13.5f, 3, 7.5f, 7.5f, 2, 2));
                g2.draw(new RoundRectangle2D.Float(3, 13.5f, 7.5f, 7.5f, 2, 2));
                g2.draw(new RoundRectangle2D.Float(13.5f, 13.5f, 7.5f, 7.5f, 2, 2));
            }
            case BOOKS -> {
                g2.draw(line(4, 4, 4, 20));
                g2.draw(path(p -> {
                    p.moveTo(4, 5.5);
                    p.curveTo(6.5, 3.6, 10.5, 3.6, 12, 5.2);
                    p.lineTo(12, 19);
                    p.curveTo(10.5, 17.6, 6.5, 17.6, 4, 19.2);
                }));
                g2.draw(path(p -> {
                    p.moveTo(20, 5.5);
                    p.curveTo(17.5, 3.6, 13.5, 3.6, 12, 5.2);
                    p.lineTo(12, 19);
                    p.curveTo(13.5, 17.6, 17.5, 17.6, 20, 19.2);
                }));
                g2.draw(line(20, 4, 20, 20));
            }
            case BOOK_OPEN -> {
                g2.draw(path(p -> {
                    p.moveTo(3.5, 5);
                    p.lineTo(9.5, 5);
                    p.curveTo(11, 5, 12, 6.2, 12, 7.6);
                    p.lineTo(12, 20);
                    p.curveTo(12, 18.8, 11, 18, 9.5, 18);
                    p.lineTo(3.5, 18);
                    p.closePath();
                }));
                g2.draw(path(p -> {
                    p.moveTo(20.5, 5);
                    p.lineTo(14.5, 5);
                    p.curveTo(13, 5, 12, 6.2, 12, 7.6);
                    p.lineTo(12, 20);
                    p.curveTo(12, 18.8, 13, 18, 14.5, 18);
                    p.lineTo(20.5, 18);
                    p.closePath();
                }));
            }
            case LIBRARY -> {
                g2.draw(line(3, 20.5f, 21, 20.5f));
                g2.draw(new RoundRectangle2D.Float(4.5f, 9, 3.6f, 11, 1, 1));
                g2.draw(new RoundRectangle2D.Float(10.2f, 6, 3.6f, 14, 1, 1));
                g2.draw(new RoundRectangle2D.Float(15.9f, 11, 3.6f, 9, 1, 1));
            }
            case CIRCULATION -> {
                g2.draw(path(p -> {
                    p.moveTo(4, 8.5);
                    p.lineTo(18, 8.5);
                }));
                g2.draw(path(p -> {
                    p.moveTo(14.5, 5);
                    p.lineTo(18, 8.5);
                    p.lineTo(14.5, 12);
                }));
                g2.draw(path(p -> {
                    p.moveTo(20, 15.5);
                    p.lineTo(6, 15.5);
                }));
                g2.draw(path(p -> {
                    p.moveTo(9.5, 12);
                    p.lineTo(6, 15.5);
                    p.lineTo(9.5, 19);
                }));
            }
            case RENEW -> {
                g2.draw(new java.awt.geom.Arc2D.Float(4, 4, 16, 16, 60, 250, java.awt.geom.Arc2D.OPEN));
                g2.draw(path(p -> {
                    p.moveTo(19.5, 3.5);
                    p.lineTo(19.2, 9);
                    p.lineTo(14, 8.2);
                }));
            }
            case MEMBERS -> {
                g2.draw(new Ellipse2D.Float(5.5f, 5, 6, 6));
                g2.draw(path(p -> {
                    p.moveTo(2.5, 20);
                    p.curveTo(2.5, 15.4, 5.4, 13.2, 8.5, 13.2);
                    p.curveTo(11.6, 13.2, 14.5, 15.4, 14.5, 20);
                }));
                g2.draw(path(p -> {
                    p.moveTo(15, 5.6);
                    p.curveTo(18, 5.6, 19.6, 7.6, 19.6, 9.6);
                    p.curveTo(19.6, 11.6, 18, 13.2, 15.6, 13.2);
                }));
                g2.draw(path(p -> {
                    p.moveTo(17, 14);
                    p.curveTo(20, 14.6, 21.5, 16.8, 21.5, 20);
                }));
            }
            case PROFILE -> {
                g2.draw(new Ellipse2D.Float(8, 4, 8, 8));
                g2.draw(path(p -> {
                    p.moveTo(3.5, 20.5);
                    p.curveTo(3.5, 15.5, 7.5, 13.5, 12, 13.5);
                    p.curveTo(16.5, 13.5, 20.5, 15.5, 20.5, 20.5);
                }));
            }
            case USER_CHECK -> {
                g2.draw(new Ellipse2D.Float(6, 4, 7.5f, 7.5f));
                g2.draw(path(p -> {
                    p.moveTo(2.5, 20.5);
                    p.curveTo(2.5, 15.8, 6, 13.5, 9.75, 13.5);
                    p.curveTo(11.4, 13.5, 12.9, 13.9, 14.1, 14.7);
                }));
                g2.draw(path(p -> {
                    p.moveTo(15, 18.2);
                    p.lineTo(17.6, 20.6);
                    p.lineTo(22, 15.4);
                }));
            }
            case LOGOUT -> {
                g2.draw(path(p -> {
                    p.moveTo(14, 4.5);
                    p.lineTo(6, 4.5);
                    p.curveTo(4.9, 4.5, 4, 5.4, 4, 6.5);
                    p.lineTo(4, 17.5);
                    p.curveTo(4, 18.6, 4.9, 19.5, 6, 19.5);
                    p.lineTo(14, 19.5);
                }));
                g2.draw(line(10, 12, 21, 12));
                g2.draw(path(p -> {
                    p.moveTo(17.5, 8.5);
                    p.lineTo(21, 12);
                    p.lineTo(17.5, 15.5);
                }));
            }
            case SEARCH -> {
                g2.draw(new Ellipse2D.Float(4, 4, 12, 12));
                g2.draw(line(15.5f, 15.5f, 20.5f, 20.5f));
            }
            case PLUS -> {
                g2.draw(line(12, 5, 12, 19));
                g2.draw(line(5, 12, 19, 12));
            }
            case EDIT -> {
                g2.draw(path(p -> {
                    p.moveTo(17.5, 3.6);
                    p.lineTo(20.4, 6.5);
                    p.lineTo(9.4, 17.5);
                    p.lineTo(5, 19);
                    p.lineTo(6.5, 14.6);
                    p.closePath();
                }));
                g2.draw(line(15.2f, 5.9f, 18.1f, 8.8f));
            }
            case TRASH -> {
                g2.draw(line(3.8f, 6.8f, 20.2f, 6.8f));
                g2.draw(path(p -> {
                    p.moveTo(5.8, 6.8);
                    p.lineTo(6.9, 19.4);
                    p.curveTo(6.9, 20.2, 7.6, 20.8, 8.4, 20.8);
                    p.lineTo(15.6, 20.8);
                    p.curveTo(16.4, 20.8, 17.1, 20.2, 17.1, 19.4);
                    p.lineTo(18.2, 6.8);
                }));
                g2.draw(path(p -> {
                    p.moveTo(9.2, 6.8);
                    p.lineTo(9.2, 4.4);
                    p.curveTo(9.2, 3.7, 9.8, 3.2, 10.5, 3.2);
                    p.lineTo(13.5, 3.2);
                    p.curveTo(14.2, 3.2, 14.8, 3.7, 14.8, 4.4);
                    p.lineTo(14.8, 6.8);
                }));
                g2.draw(line(10.4f, 10.4f, 10.4f, 17.2f));
                g2.draw(line(13.6f, 10.4f, 13.6f, 17.2f));
            }
            case REFRESH -> {
                g2.draw(new java.awt.geom.Arc2D.Float(4, 4, 16, 16, 45, 270,
                        java.awt.geom.Arc2D.OPEN));
                g2.draw(path(p -> {
                    p.moveTo(19.5, 8.5);
                    p.lineTo(19.5, 3.6);
                    p.lineTo(14.6, 3.6);
                }));
            }
            case DOWNLOAD -> {
                g2.draw(line(12, 3.5f, 12, 15.5f));
                g2.draw(path(p -> {
                    p.moveTo(7.5, 11);
                    p.lineTo(12, 15.5);
                    p.lineTo(16.5, 11);
                }));
                g2.draw(path(p -> {
                    p.moveTo(4, 18.5);
                    p.lineTo(4, 20.5);
                    p.lineTo(20, 20.5);
                    p.lineTo(20, 18.5);
                }));
            }
            case SUN -> {
                g2.draw(new Ellipse2D.Float(8, 8, 8, 8));
                for (int i = 0; i < 8; i++) {
                    double angle = Math.PI / 4 * i;
                    float inner = 9.6f;
                    float outer = 11.6f;
                    g2.draw(line(
                            (float) (12 + Math.cos(angle) * inner),
                            (float) (12 + Math.sin(angle) * inner),
                            (float) (12 + Math.cos(angle) * outer),
                            (float) (12 + Math.sin(angle) * outer)));
                }
            }
            case MOON -> g2.draw(path(p -> {
                p.moveTo(20, 15.2);
                p.curveTo(18.6, 15.9, 17, 16.2, 15.4, 15.9);
                p.curveTo(11.4, 15.2, 8.8, 11.4, 9.5, 7.4);
                p.curveTo(9.8, 5.9, 10.5, 4.6, 11.5, 3.6);
                p.curveTo(7, 4.1, 3.6, 8.1, 4.1, 12.8);
                p.curveTo(4.6, 17.5, 8.8, 20.9, 13.5, 20.4);
                p.curveTo(16.4, 20.1, 18.8, 18.1, 20, 15.2);
                p.closePath();
            }));
            case CHECK -> g2.draw(path(p -> {
                p.moveTo(4.5, 12.8);
                p.lineTo(9.5, 17.8);
                p.lineTo(19.5, 6.5);
            }));
            case CLOCK -> {
                g2.draw(new Ellipse2D.Float(3.5f, 3.5f, 17, 17));
                g2.draw(path(p -> {
                    p.moveTo(12, 7.5);
                    p.lineTo(12, 12.4);
                    p.lineTo(15.8, 14.6);
                }));
            }
            case WARNING -> {
                g2.draw(path(p -> {
                    p.moveTo(12, 3.8);
                    p.lineTo(21.5, 20.2);
                    p.lineTo(2.5, 20.2);
                    p.closePath();
                }));
                g2.draw(line(12, 9.5f, 12, 14.5f));
                g2.draw(line(12, 17.2f, 12, 17.3f));
            }
            case COIN -> {
                g2.draw(new Ellipse2D.Float(3.5f, 3.5f, 17, 17));
                g2.draw(path(p -> {
                    p.moveTo(15, 9);
                    p.curveTo(14.2, 7.8, 12.9, 7.2, 11.6, 7.2);
                    p.curveTo(9.8, 7.2, 8.6, 8.2, 8.6, 9.6);
                    p.curveTo(8.6, 12.9, 15.2, 11.1, 15.2, 14.4);
                    p.curveTo(15.2, 15.9, 13.9, 16.9, 12, 16.9);
                    p.curveTo(10.5, 16.9, 9.2, 16.2, 8.5, 15);
                }));
                g2.draw(line(12, 5.2f, 12, 18.8f));
            }
            case CHEVRON_RIGHT -> g2.draw(path(p -> {
                p.moveTo(9.5, 5.5);
                p.lineTo(16, 12);
                p.lineTo(9.5, 18.5);
            }));
        }
    }

    private static Path2D.Float line(float x1, float y1, float x2, float y2) {
        Path2D.Float path = new Path2D.Float();
        path.moveTo(x1, y1);
        path.lineTo(x2, y2);
        return path;
    }

    private static Path2D.Double path(java.util.function.Consumer<Path2D.Double> builder) {
        Path2D.Double path = new Path2D.Double();
        builder.accept(path);
        return path;
    }
}
