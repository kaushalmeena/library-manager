# Architecture

How Library Manager is put together, and why. For interface conventions see
[DESIGN.md](DESIGN.md).

## Goals

The application is a single-user desktop tool for a small library. That shapes
every decision below:

- **No server.** The whole thing is one process and one SQLite file, so it can be
  cloned and run without infrastructure.
- **Truth is derived, not stored.** Availability, overdue status and fines are
  computed from the loans table rather than kept as counters that can drift.
- **The domain is testable without a screen.** Every rule lives below the UI, so
  the test suite covers lending policy without touching Swing.

## Layers

Dependencies point in one direction only: UI → services → repositories →
database. Nothing below a layer knows the layer above it exists.

```
com.example.library
├── LibraryApp              entry point: opens the database, shows the sign-in window
├── LibraryServices         composition root: builds the object graph once
├── config/                 AppConfig — loan period, fine rate, limits, data directory
├── model/                  immutable records: Book, User, Loan, LoanDetail, …
├── db/                     Database (JDBC helper), RowMapper, DataAccessException
├── repository/             BookRepository, UserRepository, LoanRepository — SQL only
├── service/                the rules: Auth, Catalogue, Circulation, Fine, Stats, Metadata
└── ui/                     Swing: LoginFrame, MainFrame, Sidebar
    ├── theme/              Theme tokens, VectorIcon, AppIcon
    ├── component/          Card, StatCard, Badge, SearchField, CoverArt, BarChart
    ├── support/            EntityTable, FormBuilder, Async, Dialogs, Formats, CsvExport
    ├── view/               DashboardView, CatalogueView, CirculationView, MembersView, ProfileView
    └── dialog/             BookFormDialog, IssueDialog, MemberFormDialog
```

### Model

Plain `record` types with no framework annotations and no persistence awareness.
Behaviour that is purely a function of a record's own fields lives on the record
— `Loan.daysLate(today)`, `Loan.status(today, window)`,
`BookSummary.available()` — which keeps that logic close to the data and trivial
to test.

Two deliberate choices:

- **`User` carries no password material.** Hashes are read only inside
  `UserRepository.findCredentials`, so a secret cannot leak into a table model or
  a log line by accident.
- **`LoanDetail` is a projection, not an entity.** It is a loan joined with the
  book and member it refers to, which is exactly the shape the circulation tables
  render.

### Persistence

`Database` owns one long-lived JDBC connection and exposes a small set of
helpers — `query`, `queryOne`, `count`, `sum`, `update`, `insert`,
`transactional`. Every method is `synchronized`, because background workers may
touch the database concurrently and neither a `Connection` nor a
`PreparedStatement` is safe to share across threads.

Parameters are bound positionally through varargs, with `LocalDate`,
`BigDecimal` and enum values translated at the boundary. All SQL is
parameterised; no query is assembled by string concatenation.

The schema lives in `src/main/resources/db/schema.sql` and demo data in
`seed.sql`. `Database.migrate()` applies the schema on every start — the
statements are idempotent — and seeds only when the `users` table is empty. The
schema being plain SQL in version control means the data model is reviewable in
a diff rather than buried in a binary.

Repositories contain SQL and mapping, and nothing else. They never validate and
never enforce policy.

### Services

This is where the library's rules live.

| Service              | Responsibility                                                          |
| -------------------- | ----------------------------------------------------------------------- |
| `AuthService`        | Registration, sign-in, password changes, account deletion guards         |
| `CatalogueService`   | Validating and saving titles, ISBN normalisation, deletion guards         |
| `CirculationService` | Issuing, returning, renewing, fine collection, and every lending rule    |
| `FinePolicy`         | Turning lateness into money                                              |
| `StatsService`       | The aggregates the dashboards show                                       |
| `MetadataService`    | The Open Library ISBN lookup                                             |

Invalid input and blocked operations raise `ValidationException`, which carries
**one message per problem** so a form can show every complaint at once instead of
one at a time. Those messages are written for the person using the application,
which is what lets `Dialogs` render them directly.

Two details worth calling out:

**The clock is injected.** `CirculationService` takes a `Supplier<LocalDate>`.
Production passes `LocalDate::now`; tests pass a movable date, which is how the
suite can step past a due date and assert on a fine without waiting two weeks.

**Availability is transactional.** `issue` reads the copy count and inserts the
loan inside one transaction, so two desks cannot hand out the same last copy. A
partial unique index on `(book_id, user_id) WHERE return_date IS NULL` is the
database-level backstop for the same rule.

### UI

Swing, hand-written, with no generated forms. The old version was a set of
NetBeans `.form` files that could only be edited inside that IDE; laying the
interface out in code makes it reviewable, diffable and buildable anywhere.

The shell is a `MainFrame` holding a `Sidebar` and a `CardLayout` of views. Which
destinations exist depends on the signed-in role, so a student never sees a staff
screen rather than seeing one greyed out.

Three pieces carry most of the weight:

- **`View`** is the base class for screens. It provides the page title, subtitle
  and toolbar, and declares the `refresh()` hook the shell calls whenever a
  screen becomes visible or the data changes.
- **`EntityTable<T>`** is a sortable, searchable table over a list of domain
  objects. Because a row *is* the domain object, acting on a selection never
  involves parsing text back out of a cell.
- **`Async`** runs work off the event dispatch thread and delivers the result
  back on it. Every network call goes through it, so a slow ISBN lookup or cover
  download cannot freeze the window.

## Data model

```
users                     books                         loans
─────                     ─────                         ─────
id                        id                            id
name                      isbn (unique, nullable)       book_id  → books.id
email (unique)            title                         user_id  → users.id
mobile                    author                        issue_date
username (unique)         publisher                     due_date
password_hash             published_year                return_date  (NULL while out)
role                      cover_url                     renewals
created_date              price                         fine_paid
                          total_copies
                          added_date
```

`loans` is the heart of it. A row is created when a copy leaves the library and
**never deleted** — a return stamps `return_date`. That single decision is what
gives the application a borrowing history, a most-borrowed leaderboard, per-member
statistics, and fines that can be recalculated at any time.

Everything else follows from it:

- copies on loan = `COUNT(*) WHERE book_id = ? AND return_date IS NULL`
- available = `total_copies − copies on loan`
- overdue = `return_date IS NULL AND due_date < today`
- fine = chargeable late days × the daily rate

## Configuration

`AppConfig` reads system properties with sensible defaults, so policy can be
changed without a rebuild:

| Property                      | Default              | Meaning                          |
| ----------------------------- | -------------------- | -------------------------------- |
| `library.dataDir`             | `~/.library-manager` | Where the SQLite file lives      |
| `library.loanDays`            | `14`                 | Length of a loan                 |
| `library.finePerDay`          | `2.00`               | Charge per late day              |
| `library.graceDays`           | `0`                  | Late days forgiven               |
| `library.maxLoansPerMember`   | `5`                  | Concurrent loans per member       |
| `library.maxRenewals`         | `2`                  | Renewals allowed per loan        |
| `library.currency`            | `$`                  | Symbol used when showing money   |

## Testing

118 JUnit 5 tests, all running against an in-memory SQLite database built by
`TestLibrary`, which assembles the same object graph as production over a
throw-away database and a movable clock.

| Suite                      | Covers                                                     |
| -------------------------- | ---------------------------------------------------------- |
| `CirculationServiceTest`   | Issuing, returning, renewing, fines, status, delete guards  |
| `AuthServiceTest`          | Hashing, salting, sign-in, validation, admin protection     |
| `CatalogueServiceTest`     | Title validation, ISBN normalisation and duplicates         |
| `FinePolicyTest`           | Fine arithmetic, grace periods, rounding, part payments     |
| `StatsServiceTest`         | Aggregates, leaderboards, monthly activity                  |
| `MetadataServiceTest`      | Open Library parsing, against captured payloads              |
| `CsvExporterTest`          | RFC 4180 quoting and escaping                               |
| `DatabaseTest`             | Migration, seeding, foreign keys, cascades, transactions      |

No test touches the network or requires a display, so the suite runs offline on
any machine with a JDK.

## Build

Maven, with the wrapper committed so a contributor needs only a JDK.

- `./mvnw verify` — compile and test
- `./mvnw exec:java` — run from source
- `./mvnw package` — build `target/library-manager.jar`, a self-contained jar
  produced by the Shade plugin

## Decisions and trade-offs

**One shared connection instead of a pool.** A desktop application has one user.
A pool would add configuration and lifecycle for no benefit; `synchronized`
methods are enough, and SQLite serialises writes regardless.

**Plain JDBC instead of an ORM.** The whole schema is three tables. JPA or
Hibernate would outweigh the application it was mapping, and hand-written SQL
keeps the derived-availability queries explicit.

**Semicolon-splitting in the SQL script runner.** `Database.runScript` splits on
`;`, which is enough for the project's own scripts — they contain no triggers,
no `BEGIN … END` blocks and no semicolons inside string literals. A real
migration tool would be the answer if the schema ever needed those.

**No reservations yet.** A hold queue is the obvious next feature and would need
a fourth table; it was left out rather than half-built.

**Cover art is cached in memory only.** Covers are fetched once per URL into a
bounded LRU map and lost on exit. Persisting them would mean managing an image
cache on disk for very little gain.
