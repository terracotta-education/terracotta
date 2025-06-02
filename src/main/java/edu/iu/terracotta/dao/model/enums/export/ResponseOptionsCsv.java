package edu.iu.terracotta.dao.model.enums.export;

import java.util.Arrays;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum ResponseOptionsCsv {

    // NOTE: order is important!
    RESPONSE_ID("response_id"),
    ITEM_ID("item_id"),
    RESPONSE("response"),
    REPONSE_POSITION("response_position"),
    CORRECT("correct"),
    RANDOMIZED("randomized");

    public static final String FILENAME = "response_options.csv";

    private String header;

    @Override
    public String toString() {
        return header;
    }

    public static String[] getHeaderRow() {
        return Arrays.stream(values())
            .map(ResponseOptionsCsv::toString)
            .toArray(String[]::new);
    }

}
