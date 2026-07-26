package com.example.library.service;

import com.example.library.model.BookMetadata;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Looks bibliographic details up by ISBN against the Open Library API, so a librarian can
 * catalogue a book by typing its barcode instead of retyping the title page.
 *
 * <p>No API key is needed. Parsing is split out into {@link #parse(String, String)} so the
 * response handling can be tested without touching the network.
 */
public final class MetadataService {

    private static final String ENDPOINT =
            "https://openlibrary.org/api/books?bibkeys=ISBN:%s&format=json&jscmd=data";

    /** Cover artwork is addressable directly by ISBN, sized S, M or L. */
    private static final String COVER_ENDPOINT = "https://covers.openlibrary.org/b/isbn/%s-M.jpg";

    private static final Pattern YEAR = Pattern.compile("(1[0-9]{3}|20[0-9]{2})");

    private final HttpClient httpClient;
    private final Duration timeout;

    public MetadataService() {
        this(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build(),
                Duration.ofSeconds(12));
    }

    public MetadataService(HttpClient httpClient, Duration timeout) {
        this.httpClient = httpClient;
        this.timeout = timeout;
    }

    /** Raised when the lookup cannot be completed, as opposed to completing with no match. */
    public static class LookupException extends RuntimeException {
        public LookupException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * Fetches details for an ISBN.
     *
     * <p>Must not be called on the Swing event dispatch thread.
     *
     * @param rawIsbn an ISBN, with or without separators
     * @return the resolved details, or empty when Open Library has no record of the ISBN
     * @throws ValidationException when the ISBN is malformed
     * @throws LookupException     when the request fails or is interrupted
     */
    public Optional<BookMetadata> lookupByIsbn(String rawIsbn) {
        String isbn = CatalogueService.normaliseIsbn(rawIsbn);
        if (!CatalogueService.isValidIsbn(isbn)) {
            throw new ValidationException(
                    "Enter a 10 or 13 digit ISBN before looking up the details.");
        }
        HttpRequest request = HttpRequest.newBuilder(URI.create(String.format(ENDPOINT, isbn)))
                .header("Accept", "application/json")
                .header("User-Agent", "library-manager/2.0 (https://github.com/kaushalmeena)")
                .timeout(timeout)
                .GET()
                .build();
        try {
            HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new LookupException(
                        "Open Library returned HTTP " + response.statusCode() + ".", null);
            }
            return parse(response.body(), isbn);
        } catch (IOException e) {
            throw new LookupException("Could not reach Open Library. Check your connection.", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new LookupException("The lookup was interrupted.", e);
        }
    }

    /**
     * Extracts the fields the catalogue form needs from an Open Library {@code jscmd=data}
     * response.
     *
     * @param json the raw response body
     * @param isbn the ISBN that was requested, used to key into the response
     * @return the resolved details, or empty when the response holds no entry for the ISBN
     */
    public static Optional<BookMetadata> parse(String json, String isbn) {
        if (json == null || json.isBlank()) {
            return Optional.empty();
        }
        JsonObject root;
        try {
            JsonElement parsed = JsonParser.parseString(json);
            if (!parsed.isJsonObject()) {
                return Optional.empty();
            }
            root = parsed.getAsJsonObject();
        } catch (JsonSyntaxException e) {
            return Optional.empty();
        }

        // Open Library keys the response by the bibkey it was asked for, e.g. "ISBN:9780134685991".
        JsonElement entry = root.get("ISBN:" + isbn);
        if (entry == null || !entry.isJsonObject()) {
            return Optional.empty();
        }
        JsonObject data = entry.getAsJsonObject();

        String title = string(data, "title");
        if (title == null) {
            return Optional.empty();
        }
        String subtitle = string(data, "subtitle");
        if (subtitle != null) {
            title = title + ": " + subtitle;
        }

        return Optional.of(new BookMetadata(
                isbn,
                title,
                firstNamed(data, "authors"),
                firstNamed(data, "publishers"),
                parseYear(string(data, "publish_date")),
                coverUrl(data, isbn)));
    }

    /** Reads the {@code name} of the first element of an array such as {@code authors}. */
    private static String firstNamed(JsonObject data, String arrayField) {
        JsonElement element = data.get(arrayField);
        if (element == null || !element.isJsonArray()) {
            return null;
        }
        JsonArray array = element.getAsJsonArray();
        if (array.isEmpty() || !array.get(0).isJsonObject()) {
            return null;
        }
        return string(array.get(0).getAsJsonObject(), "name");
    }

    /** Prefers the cover the response supplies, falling back to the ISBN cover endpoint. */
    private static String coverUrl(JsonObject data, String isbn) {
        JsonElement cover = data.get("cover");
        if (cover != null && cover.isJsonObject()) {
            JsonObject sizes = cover.getAsJsonObject();
            for (String size : new String[]{"medium", "large", "small"}) {
                String url = string(sizes, size);
                if (url != null) {
                    return url;
                }
            }
        }
        return String.format(COVER_ENDPOINT, isbn);
    }

    /** Pulls a four-digit year out of free-form dates like "December 27, 2017". */
    static Integer parseYear(String publishDate) {
        if (publishDate == null) {
            return null;
        }
        Matcher matcher = YEAR.matcher(publishDate);
        return matcher.find() ? Integer.valueOf(matcher.group(1)) : null;
    }

    private static String string(JsonObject object, String field) {
        JsonElement element = object.get(field);
        if (element == null || !element.isJsonPrimitive()) {
            return null;
        }
        String value = element.getAsString().trim();
        return value.isEmpty() ? null : value;
    }

    /** The conventional cover URL for an ISBN, used when cataloguing without a lookup. */
    public static String coverUrlFor(String isbn) {
        String normalised = CatalogueService.normaliseIsbn(isbn);
        return CatalogueService.isValidIsbn(normalised)
                ? String.format(COVER_ENDPOINT, normalised)
                : null;
    }
}
