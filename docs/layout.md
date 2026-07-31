# Layout and components

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
