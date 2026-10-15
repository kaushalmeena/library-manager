# Library Manager

A Java 17 Swing desktop application for running a small library, storing
everything in a local SQLite file. One user, one process, no server — several
decisions below only make sense in that light.

## Done means the build passes

```bash
./mvnw verify
```

That compiles and runs the JUnit 5 suite (119 tests) against a throw-away
in-memory database; the tests make no network calls. Use `./mvnw exec:java` to
launch the app, and `mvnw.cmd` in place of `./mvnw` on Windows. Maven itself is
not required — the wrapper is committed.

## Reading the codebase

| Working on                                     | Read                                      |
| ---------------------------------------------- | ----------------------------------------- |
| Goals, layer boundaries                        | [overview.md](docs/overview.md)           |
| Tables, derived availability, tunable settings | [data-model.md](docs/data-model.md)       |
| The suite, packaging                           | [testing.md](docs/testing.md)             |
| Why something was built this way               | [decisions.md](docs/decisions.md)         |
| Principles, colour, type, iconography          | [foundations.md](docs/foundations.md)     |
| Screen arrangement, components                 | [layout.md](docs/layout.md)               |
| Input, validation, confirmation                | [forms.md](docs/forms.md)                 |
| Dark mode, keyboard and screen-reader rules    | [accessibility.md](docs/accessibility.md) |

## Conventions

**Availability is derived, never stored.** How many copies are on the shelf is
computed from open loans rather than kept as a counter, so it cannot drift out
of step with the loans table. Do not add a cached count.

**Returning closes a loan; it never deletes one.** The borrowing history is
permanent, and the dashboards read from it.

**One shared connection, not a pool.** A desktop app has one user, `synchronized`
methods are enough, and SQLite serialises writes anyway.

**Plain JDBC, not an ORM.** The schema is three tables; hand-written SQL keeps
the derived-availability queries explicit.

**Long work goes off the UI thread.** The ISBN lookup runs in the background so
a slow network never freezes the window.

## Gotchas

- `Database.runScript` splits SQL on `;`. That is enough for the project's own
  scripts — no triggers, no `BEGIN … END`, no semicolons inside string literals.
  A schema needing those needs a real migration tool instead.
- Cover art is cached in a bounded in-memory LRU map and lost on exit. There is
  deliberately no on-disk image cache.
- Library policy (loan length, fine per day) is read from system properties, so
  it can be changed without a rebuild:
  `./mvnw exec:java -Dlibrary.loanDays=7 -Dlibrary.finePerDay=5.00`.
- All data lives in `~/.library-manager/`. Delete that directory to start over.
