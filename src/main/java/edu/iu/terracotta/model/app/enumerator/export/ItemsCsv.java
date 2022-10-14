package edu.iu.terracotta.model.app.enumerator.export;

import java.util.Arrays;

public enum ItemsCsv {

    // NOTE: order is important!
    ITEM_ID("item_id"),
    ASSIGNMENT_ID("assignment_id"),
    TREATMENT_ID("treatment_id"),
    CONDITION_ID("condition_id"),
    ITEM_TEXT("item_text"),
    ITEM_FORMAT("item_format");

    public static final String FILENAME = "items.csv";

    private String header;

    private ItemsCsv(String header) {
        this.header = header;
    }

    @Override
    public String toString() {
        return header;
    }

    public static String[] getHeaderRow() {
        return Arrays.stream(ItemsCsv.values()).map(ItemsCsv::toString).toArray(String[]::new);
    }

}
