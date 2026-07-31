# Data model and configuration

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
