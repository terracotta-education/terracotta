package edu.iu.terracotta.dao.model.enums.export;

import java.util.Arrays;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum ParticipantsCsv {

    // NOTE: order is important!
    PARTICIPANT_ID("participant_id"),
    CONSENTED_AT("consented_at"),
    CONSENT_SOURCE("consent_source");

    public static final String FILENAME = "participants.csv";

    private String header;

    @Override
    public String toString() {
        return header;
    }

    public static String[] getHeaderRow() {
        return Arrays.stream(values())
            .map(ParticipantsCsv::toString)
            .toArray(String[]::new);
    }

}
