# Foundations

## Principles

**Act on a selection, never on a typed identifier.** The previous version asked
the librarian to type a book id into a text field and validated it with a regular
expression, which meant a typo could aim an operation at the wrong record. Every
destructive or state-changing action is now driven by the row that is selected,
and the buttons that need a selection stay disabled until there is one.

**Say what will happen before it happens.** The issue dialog shows the due date
that will be applied and any rule that would block the loan while you are still
choosing. The return dialog states the fine before asking for confirmation.
Nothing surprises you after you commit.

**Never block the window.** Anything that touches the network — the ISBN lookup,
cover downloads — runs on a background thread through `Async` and delivers its
result back on the event dispatch thread.

**One rule, one home.** Lending policy lives in the services. The UI may check a
rule early to show a warning, but it never becomes the only place that rule is
enforced.

## Visual language

The look comes from [FlatLaf](https://www.formdev.com/flatlaf/) with a small set
of project tokens on top, all defined in `ui/theme/Theme.java`. Screens ask for
semantic values — `Theme.danger()`, `Theme.surface()`, `Theme.SPACE_4` — rather
than hard-coding colours or pixel gaps, which is what keeps light and dark mode
in step.

### Colour

Every colour is declared as a light/dark pair and resolved through the active
mode, so a screen never needs to know which theme is on.

| Token                   | Role                                          |
| ----------------------- | --------------------------------------------- |
| `accent`                | Primary actions, active navigation item        |
| `accentSoft`            | Selected table rows, tinted icon wells         |
| `canvas`                | Window background                              |
| `surface` / `surfaceSunken` | Cards; the sidebar and table headers       |
| `border` / `divider`    | Card outlines; row separators                  |
| `textPrimary` / `textSecondary` / `textMuted` | Three-step text hierarchy |
| `success` `warning` `danger` `info` | Status meaning                     |

Semantic colour is never the only signal. A status badge carries its label as
well as its tint, so an overdue loan is readable without relying on colour
perception.

### Type

One scale, derived from the look and feel's base font so it follows the platform:

| Token           | Use                                          |
| --------------- | -------------------------------------------- |
| `titleFont`     | Page and dialog titles                        |
| `headingFont`   | Card headings                                 |
| `metricFont`    | The large number on a stat card               |
| `bodyFont` / `bodyBoldFont` | Table cells, labels, buttons      |
| `smallFont` / `smallBoldFont` | Captions, table headers, badges |

### Spacing

A six-step scale (`4, 8, 12, 16, 24, 32`) used for every gap, padding and inset.
Layout code refers to the steps rather than raw numbers, which is why panels line
up across unrelated screens.

### Icons

The icon set is drawn with Java2D in `ui/theme/VectorIcon.java` rather than
shipped as image files. Each glyph is described on a 24×24 grid and scaled to the
requested size, so icons stay sharp on any display and take the current theme
colour without needing light and dark copies of every asset.

The application icon is the exception, because it has to exist outside the
running interface. It is authored once as [`assets/logo.svg`](../assets/logo.svg)
— the same file the README displays — and `ui/theme/AppIcon.java` rasterises that
file at the sizes the platform asks for, rather than redrawing the mark in code
where the two copies would drift apart.
