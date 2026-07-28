package edu.iu.terracotta.dao.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import edu.iu.terracotta.dao.model.enums.RegradeOption;

/**
 * {@link RegradeDetails} is a plain (non-{@code @Entity}) Lombok {@code @Builder} class. These
 * tests exercise its two hand-written getters that null-default to {@link RegradeOption#NA} and
 * an empty list, respectively.
 */
public class RegradeDetailsTest {

    @Test
    public void testGetRegradeOptionDefaultsToNaWhenNull() {
        RegradeDetails regradeDetails = RegradeDetails.builder().build();

        assertEquals(RegradeOption.NA, regradeDetails.getRegradeOption());
    }

    @Test
    public void testGetEditedMCQuestionIdsDefaultsToEmptyListWhenNull() {
        RegradeDetails regradeDetails = RegradeDetails.builder().build();

        List<Long> editedMCQuestionIds = regradeDetails.getEditedMCQuestionIds();

        assertNotNull(editedMCQuestionIds);
        assertTrue(editedMCQuestionIds.isEmpty());
    }

    @Test
    public void testGetRegradeOptionReturnsSetValue() {
        RegradeDetails regradeDetails = RegradeDetails.builder()
            .regradeOption(RegradeOption.BOTH)
            .build();

        assertEquals(RegradeOption.BOTH, regradeDetails.getRegradeOption());
    }

    @Test
    public void testGetEditedMCQuestionIdsReturnsSetValue() {
        RegradeDetails regradeDetails = RegradeDetails.builder()
            .editedMCQuestionIds(List.of(1L, 2L))
            .build();

        assertEquals(List.of(1L, 2L), regradeDetails.getEditedMCQuestionIds());
    }

}
