package edu.iu.terracotta.model.app.enumerator.export;

import java.util.Arrays;

public enum ParticipantsCsv {

    // NOTE: order is important!
    PARTICIPANT_ID("participant_id"),
    CONSENTED_AT("consented_at"),
    CONSENT_SOURCE("consent_source");

    public static final String FILENAME = "participants.csv";

    private String header;

    ParticipantsCsv(String header) {
        this.header = header;
    }

    @Override
    public String toString() {
        return header;
    }

    public static String[] getHeaderRow() {
        return Arrays.stream(ParticipantsCsv.values()).map(ParticipantsCsv::toString).toArray(String[]::new);
    }

}
