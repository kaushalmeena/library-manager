package com.example.library;

import com.example.library.config.AppConfig;
import com.example.library.db.Database;
import com.example.library.repository.BookRepository;
import com.example.library.repository.LoanRepository;
import com.example.library.repository.UserRepository;
import com.example.library.service.AuthService;
import com.example.library.service.CatalogueService;
import com.example.library.service.CirculationService;
import com.example.library.service.FinePolicy;
import com.example.library.service.MetadataService;
import com.example.library.service.StatsService;

/**
 * Composition root: builds the object graph once and hands it to the interface.
 *
 * <p>Keeping the wiring in one place means the screens receive the services they need instead
 * of reaching for global state, and the same graph can be assembled over a temporary database
 * in the tests.
 */
public final class LibraryServices implements AutoCloseable {

    private final AppConfig config;
    private final Database database;
    private final UserRepository userRepository;
    private final BookRepository bookRepository;
    private final LoanRepository loanRepository;
    private final AuthService authService;
    private final CatalogueService catalogueService;
    private final CirculationService circulationService;
    private final StatsService statsService;
    private final MetadataService metadataService;
    private final boolean seeded;

    private LibraryServices(AppConfig config, Database database) {
        this.config = config;
        this.database = database;
        this.seeded = database.migrate();

        this.userRepository = new UserRepository(database);
        this.bookRepository = new BookRepository(database);
        this.loanRepository = new LoanRepository(database);

        FinePolicy finePolicy = new FinePolicy(config);
        this.authService = new AuthService(userRepository);
        this.catalogueService = new CatalogueService(bookRepository, loanRepository);
        this.circulationService = new CirculationService(database, bookRepository, userRepository,
                loanRepository, finePolicy, config);
        this.statsService = new StatsService(bookRepository, userRepository, loanRepository,
                circulationService);
        this.metadataService = new MetadataService();
    }

    /** Opens the database in the configured data directory and wires everything up. */
    public static LibraryServices create(AppConfig config) {
        return new LibraryServices(config, Database.openFile(config.databaseFile()));
    }

    /** Wires everything up over a throw-away in-memory database. */
    public static LibraryServices createInMemory(AppConfig config) {
        return new LibraryServices(config, Database.openInMemory());
    }

    /** Whether this run created and seeded a brand new database. */
    public boolean wasSeeded() {
        return seeded;
    }

    public AppConfig config() {
        return config;
    }

    public Database database() {
        return database;
    }

    public UserRepository userRepository() {
        return userRepository;
    }

    public BookRepository bookRepository() {
        return bookRepository;
    }

    public LoanRepository loanRepository() {
        return loanRepository;
    }

    public AuthService authService() {
        return authService;
    }

    public CatalogueService catalogueService() {
        return catalogueService;
    }

    public CirculationService circulationService() {
        return circulationService;
    }

    public StatsService statsService() {
        return statsService;
    }

    public MetadataService metadataService() {
        return metadataService;
    }

    @Override
    public void close() {
        database.close();
    }
}
