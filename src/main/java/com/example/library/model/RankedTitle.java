package com.example.library.model;

/**
 * A leaderboard entry, used for the dashboard's most-borrowed list.
 *
 * @param label     what is being counted, e.g. a book title or a member name
 * @param sublabel  secondary line, e.g. the author or the member's email
 * @param count     number of loans behind the ranking
 */
public record RankedTitle(String label, String sublabel, int count) {
}
