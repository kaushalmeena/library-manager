# Testing and build

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
