package com.example.library.model;

/**
 * Bibliographic details resolved from an external catalogue for a given ISBN.
 *
 * @param isbn          the ISBN that was looked up
 * @param title         resolved title
 * @param author        first listed author, {@code null} when the source lists none
 * @param publisher     first listed publisher, {@code null} when the source lists none
 * @param publishedYear four-digit year parsed from the source's publish date
 * @param coverUrl      cover artwork URL, {@code null} when the source has no cover
 */
public record BookMetadata(
        String isbn,
        String title,
        String author,
        String publisher,
        Integer publishedYear,
        String coverUrl) {
}
