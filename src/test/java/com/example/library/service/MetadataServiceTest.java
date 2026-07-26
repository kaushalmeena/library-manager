package com.example.library.service;

import com.example.library.model.BookMetadata;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Exercises the Open Library response handling against captured payloads, so the parsing is
 * covered without the test suite depending on the network.
 */
class MetadataServiceTest {

    private static final String ISBN = "9780134685991";

    /** Trimmed from a real {@code jscmd=data} response. */
    private static final String FULL_RESPONSE = """
            {
              "ISBN:9780134685991": {
                "url": "https://openlibrary.org/books/OL31838212M/Effective_Java",
                "title": "Effective Java",
                "authors": [{"url": "https://openlibrary.org/authors/OL1607920A", "name": "Joshua Bloch"}],
                "number_of_pages": 416,
                "publishers": [{"name": "Addison-Wesley Professional"}],
                "publish_date": "December 27, 2017",
                "cover": {
                  "small": "https://covers.openlibrary.org/b/id/10520611-S.jpg",
                  "medium": "https://covers.openlibrary.org/b/id/10520611-M.jpg",
                  "large": "https://covers.openlibrary.org/b/id/10520611-L.jpg"
                }
              }
            }
            """;

    @Test
    @DisplayName("pulls the title, author, publisher, year and cover out of a full response")
    void parsesFullResponse() {
        BookMetadata metadata = MetadataService.parse(FULL_RESPONSE, ISBN).orElseThrow();

        assertEquals(ISBN, metadata.isbn());
        assertEquals("Effective Java", metadata.title());
        assertEquals("Joshua Bloch", metadata.author());
        assertEquals("Addison-Wesley Professional", metadata.publisher());
        assertEquals(2017, metadata.publishedYear());
        assertEquals("https://covers.openlibrary.org/b/id/10520611-M.jpg", metadata.coverUrl());
    }

    @Test
    @DisplayName("appends a subtitle to the title")
    void appendsSubtitle() {
        String json = """
                {"ISBN:9780134685991": {"title": "Refactoring",
                 "subtitle": "Improving the Design of Existing Code"}}
                """;

        BookMetadata metadata = MetadataService.parse(json, ISBN).orElseThrow();

        assertEquals("Refactoring: Improving the Design of Existing Code", metadata.title());
    }

    @Test
    @DisplayName("returns nothing when the response holds no entry for the ISBN")
    void handlesUnknownIsbn() {
        assertTrue(MetadataService.parse("{}", ISBN).isEmpty());
        assertTrue(MetadataService.parse("{\"ISBN:0000000000\": {\"title\": \"Other\"}}", ISBN)
                .isEmpty());
    }

    @Test
    @DisplayName("returns nothing rather than failing on a malformed response")
    void handlesMalformedResponse() {
        assertTrue(MetadataService.parse("not json at all", ISBN).isEmpty());
        assertTrue(MetadataService.parse("[1, 2, 3]", ISBN).isEmpty());
        assertTrue(MetadataService.parse("", ISBN).isEmpty());
        assertTrue(MetadataService.parse(null, ISBN).isEmpty());
    }

    @Test
    @DisplayName("returns nothing when the entry has no title to catalogue")
    void requiresTitle() {
        String json = "{\"ISBN:9780134685991\": {\"publishers\": [{\"name\": \"Nobody\"}]}}";

        assertTrue(MetadataService.parse(json, ISBN).isEmpty());
    }

    @Test
    @DisplayName("copes with a response that omits authors, publishers, date and cover")
    void handlesSparseResponse() {
        String json = "{\"ISBN:9780134685991\": {\"title\": \"A Sparse Record\"}}";

        BookMetadata metadata = MetadataService.parse(json, ISBN).orElseThrow();

        assertEquals("A Sparse Record", metadata.title());
        assertNull(metadata.author());
        assertNull(metadata.publisher());
        assertNull(metadata.publishedYear());
        // With no cover in the payload, the conventional ISBN cover URL is offered instead.
        assertEquals("https://covers.openlibrary.org/b/isbn/9780134685991-M.jpg",
                metadata.coverUrl());
    }

    @Test
    @DisplayName("falls back through the cover sizes the response does provide")
    void fallsBackThroughCoverSizes() {
        String json = """
                {"ISBN:9780134685991": {"title": "Only Large",
                 "cover": {"large": "https://example.test/large.jpg"}}}
                """;

        assertEquals("https://example.test/large.jpg",
                MetadataService.parse(json, ISBN).orElseThrow().coverUrl());
    }

    @Test
    @DisplayName("takes the first author and publisher when several are listed")
    void takesFirstOfEach() {
        String json = """
                {"ISBN:9780134685991": {"title": "Many Hands",
                 "authors": [{"name": "First Author"}, {"name": "Second Author"}],
                 "publishers": [{"name": "First Publisher"}, {"name": "Second Publisher"}]}}
                """;

        BookMetadata metadata = MetadataService.parse(json, ISBN).orElseThrow();

        assertEquals("First Author", metadata.author());
        assertEquals("First Publisher", metadata.publisher());
    }

    @ParameterizedTest
    @DisplayName("finds a four-digit year inside a free-form publish date")
    @CsvSource({
            "'December 27, 2017', 2017",
            "2008, 2008",
            "'March 1994', 1994",
            "'1994-05-01', 1994",
            "'no year here', ",
            "'', "
    })
    void parsesYear(String publishDate, Integer expected) {
        assertEquals(expected, MetadataService.parseYear(publishDate));
    }

    @ParameterizedTest
    @NullSource
    @DisplayName("treats a missing publish date as an unknown year")
    void parsesNullYear(String publishDate) {
        assertNull(MetadataService.parseYear(publishDate));
    }

    @Test
    @DisplayName("refuses to look up a malformed ISBN before making a request")
    void rejectsMalformedIsbnBeforeRequest() {
        MetadataService service = new MetadataService();

        ValidationException thrown = assertThrows(ValidationException.class,
                () -> service.lookupByIsbn("12345"));

        assertTrue(thrown.getMessage().contains("ISBN"), thrown.getMessage());
    }

    @Test
    @DisplayName("builds the conventional cover URL only for a valid ISBN")
    void buildsCoverUrl() {
        assertEquals("https://covers.openlibrary.org/b/isbn/9780134685991-M.jpg",
                MetadataService.coverUrlFor("978-0-13-468599-1"));
        assertNull(MetadataService.coverUrlFor("nonsense"));
        assertNull(MetadataService.coverUrlFor(null));
    }

    @Test
    @DisplayName("keys the response by the exact bibkey that was requested")
    void keysByRequestedIsbn() {
        // A 10-digit request must not accidentally match the 13-digit entry.
        Optional<BookMetadata> mismatched = MetadataService.parse(FULL_RESPONSE, "0134685997");

        assertTrue(mismatched.isEmpty());
    }
}
