-- Library Manager schema (version 1)
--
-- Applied by com.example.library.db.Database on first run. Statements are
-- idempotent so the file doubles as documentation of the live data model.

CREATE TABLE IF NOT EXISTS users (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    name          TEXT    NOT NULL,
    email         TEXT    NOT NULL UNIQUE COLLATE NOCASE,
    mobile        TEXT,
    username      TEXT    NOT NULL UNIQUE COLLATE NOCASE,
    password_hash TEXT    NOT NULL,
    role          TEXT    NOT NULL DEFAULT 'STUDENT'
                          CHECK (role IN ('ADMIN', 'LIBRARIAN', 'STUDENT')),
    created_date  TEXT    NOT NULL DEFAULT (date('now'))
);

CREATE TABLE IF NOT EXISTS books (
    id             INTEGER PRIMARY KEY AUTOINCREMENT,
    isbn           TEXT    UNIQUE,
    title          TEXT    NOT NULL,
    author         TEXT,
    publisher      TEXT,
    published_year INTEGER,
    cover_url      TEXT,
    price          REAL    NOT NULL DEFAULT 0    CHECK (price >= 0),
    total_copies   INTEGER NOT NULL DEFAULT 1    CHECK (total_copies >= 0),
    added_date     TEXT    NOT NULL DEFAULT (date('now'))
);

-- A loan row is never deleted. Returning a book stamps return_date, which is
-- what gives the application a full circulation history.
CREATE TABLE IF NOT EXISTS loans (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    book_id     INTEGER NOT NULL REFERENCES books(id) ON DELETE CASCADE,
    user_id     INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    issue_date  TEXT    NOT NULL DEFAULT (date('now')),
    due_date    TEXT    NOT NULL,
    return_date TEXT,
    renewals    INTEGER NOT NULL DEFAULT 0 CHECK (renewals >= 0),
    fine_paid   REAL    NOT NULL DEFAULT 0 CHECK (fine_paid >= 0)
);

CREATE INDEX IF NOT EXISTS idx_loans_book ON loans (book_id);
CREATE INDEX IF NOT EXISTS idx_loans_user ON loans (user_id);
CREATE INDEX IF NOT EXISTS idx_loans_outstanding ON loans (return_date);

-- One user may not hold two simultaneous copies of the same title.
CREATE UNIQUE INDEX IF NOT EXISTS idx_loans_unique_open
    ON loans (book_id, user_id) WHERE return_date IS NULL;

CREATE INDEX IF NOT EXISTS idx_books_title ON books (title);
