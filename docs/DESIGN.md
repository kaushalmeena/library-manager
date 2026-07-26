# Design

The interface conventions Library Manager follows, and the reasoning behind them.
For how the code is structured see [ARCHITECTURE.md](ARCHITECTURE.md).

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
colour without needing light and dark copies of every asset. The application and
dock icon in `ui/theme/AppIcon.java` paints the same mark as
[`assets/logo.svg`](../assets/logo.svg).

## Layout

### The shell

A fixed sidebar on the left, one screen at a time on the right.

```
┌──────────────┬────────────────────────────────────────────────┐
│  Library     │  Page title                    [ toolbar ]     │
│              │  subtitle with live counts                     │
│  Dashboard   │  ┌──────────────────────────────────────────┐  │
│  Catalogue   │  │                                          │  │
│  Circulation │  │   card                                   │  │
│  Members     │  │                                          │  │
│  Profile     │  └──────────────────────────────────────────┘  │
│              │                                                │
│  ── AL ──    │                                                │
│  Ada L.      │                                                │
│  Dark mode   │                                                │
│  Sign out    │                                                │
└──────────────┴────────────────────────────────────────────────┘
```

The sidebar was chosen over a tab strip because the destinations differ by role:
a student sees three items, an admin five. Tabs would either show disabled tabs
or change width unpredictably.

The active item is a filled pill in the accent colour, and the circulation entry
carries a count of overdue loans so the number is visible from any screen.

### Page furniture

Every screen extends `View`, which supplies the same header geometry: title on
the left, a live subtitle beneath it, toolbar actions on the right. The subtitle
is where counts go — "15 titles · 38 copies · 24 on the shelf" — because a
heading that reports the current state is more useful than one that repeats the
navigation label.

### Cards

Content sits on rounded, bordered surfaces painted by `Card`. The background is
painted by the component rather than applied as a border so the corners render
cleanly against the canvas.

## Components

**Stat card** — an uppercase label, a large number, a supporting line, and a
tinted icon well. The supporting line always adds context the number alone
lacks: `24` on its own is meaningless, `24 / 63% of the collection is out` is not.

**Status badge** — a tinted pill. Loan status has five states and each maps to a
semantic tone:

| Status          | Tone    | Meaning                             |
| --------------- | ------- | ----------------------------------- |
| `ON_LOAN`       | info    | Out, comfortably within its period   |
| `DUE_SOON`      | warning | Out, due within three days           |
| `OVERDUE`       | danger  | Out, past its due date               |
| `RETURNED`      | success | Came back on time                    |
| `RETURNED_LATE` | neutral | Came back late; fine already settled  |

`RETURNED_LATE` is deliberately neutral rather than red: the book is back and the
matter is closed, so it should not compete for attention with something still
out.

**Tables** — horizontal rules only, no vertical grid, 34px rows, muted small-caps
headers. Sorting is on every column with a comparator that respects the
underlying type, so dates and amounts sort properly instead of alphabetically.
Search filters on the text that is actually displayed, so what you type matches
what you can see.

**Empty states** — a table with no rows says something useful. "The catalogue is
empty. Add the first title to get started." when there is no data, and "No rows
match your search." when a filter is hiding everything, because those are
different problems.

**Cover art** — downloaded in the background, cached per URL for the session, and
centre-cropped to fill its frame without distortion. While it loads, or when a
book has no artwork, a drawn placeholder stands in rather than a blank rectangle.

**Bar chart** — six months of loan activity, scaled to the largest month, with
the newest month in the accent colour. Empty months are drawn as a sliver rather
than omitted, so the spacing stays even and a quiet month is visibly a quiet
month rather than a gap.

## Forms

`FormBuilder` lays out label-above-field forms on a grid, which is what keeps
gaps, label styling and helper text identical everywhere. Helper text sits under
the field it explains and states the rule up front — "3-20 characters", "Leave
blank to keep the current password" — rather than waiting for a failed submit.

Validation follows one pattern throughout:

1. The form parses only what it must, such as a year that is not a number, since
   that has to be reported before the value can reach a service at all.
2. Everything else goes to the service, which raises `ValidationException`
   carrying one message per problem.
3. `Dialogs.showValidationProblems` renders those as a bulleted list, so all
   complaints appear at once.

Messages name the fix, not the failure: "Password must be at least 8 characters"
rather than "invalid password".

## Dialogs

Four kinds, all through `Dialogs`, so tone and layout stay consistent:

- **Info and success** — confirm what happened, naming the record involved.
- **Warning** — something is blocked and explains what must happen first.
- **Confirm** — a question with the action itself as the button label. "Return"
  and "Remove", never "OK".
- **Destructive confirm** — the same, but defaulted to Cancel, so a stray Return
  keypress cannot delete a record.

Every message is escaped and wrapped in a fixed-width HTML label so long titles
break sensibly instead of stretching the dialog across the screen.

## Dark mode

Both themes are first-class rather than an inversion. Colours are chosen per mode:
the accent lightens in dark mode so it keeps contrast against a dark surface, and
tint alpha rises so badges remain legible. The preference is stored in Java
`Preferences` and reapplied at startup, and the toggle restyles every open window
live.

## Accessibility

- Contrast: body text against its surface meets WCAG AA in both themes.
- Colour is never the sole carrier of meaning — every badge has a text label.
- Each dialog and form sets a default button, so Return submits.
- All controls are reachable by keyboard, and tables are fully navigable.
- Icons are decorative and always sit beside a text label, never alone as the
  only meaning of a control.
