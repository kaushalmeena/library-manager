# Dark mode and accessibility

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
