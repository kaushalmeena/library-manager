# Decisions and trade-offs

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
