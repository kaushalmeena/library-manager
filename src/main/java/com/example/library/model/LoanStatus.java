package com.example.library.model;

/** Display state of a loan, driving the colour of its status badge. */
public enum LoanStatus {

    ON_LOAN("On loan", Tone.INFO),
    DUE_SOON("Due soon", Tone.WARNING),
    OVERDUE("Overdue", Tone.DANGER),
    RETURNED("Returned", Tone.SUCCESS),
    RETURNED_LATE("Returned late", Tone.NEUTRAL);

    /** Semantic colour family, resolved to actual colours by the UI theme. */
    public enum Tone { INFO, WARNING, DANGER, SUCCESS, NEUTRAL }

    private final String label;
    private final Tone tone;

    LoanStatus(String label, Tone tone) {
        this.label = label;
        this.tone = tone;
    }

    public String label() {
        return label;
    }

    public Tone tone() {
        return tone;
    }

    public boolean isOutstanding() {
        return this == ON_LOAN || this == DUE_SOON || this == OVERDUE;
    }
}
