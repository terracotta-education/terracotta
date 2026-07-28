package edu.iu.terracotta.dao.entity;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Timestamp;
import java.util.List;

import org.junit.jupiter.api.Test;

import edu.iu.terracotta.dao.model.enums.ExposureTypes;

/**
 * {@link Experiment} is a Lombok {@code @Builder} JPA entity. These tests exercise the three
 * hand-written {@code @Transient} helper methods: {@code isStarted()}, {@code canSetExposureType()},
 * and {@code isSingleCondition()}.
 */
public class ExperimentTest {

    @Test
    public void testIsStartedFalseWhenNull() {
        Experiment experiment = Experiment.builder()
            .started(null)
            .build();

        assertFalse(experiment.isStarted());
    }

    @Test
    public void testIsStartedTrueWhenNonNull() {
        Experiment experiment = Experiment.builder()
            .started(new Timestamp(System.currentTimeMillis()))
            .build();

        assertTrue(experiment.isStarted());
    }

    @Test
    public void testCanSetExposureTypeTrueWhenNoset() {
        Experiment experiment = Experiment.builder()
            .exposureType(ExposureTypes.NOSET)
            .build();

        assertTrue(experiment.canSetExposureType());
    }

    @Test
    public void testCanSetExposureTypeFalseWhenBetween() {
        Experiment experiment = Experiment.builder()
            .exposureType(ExposureTypes.BETWEEN)
            .build();

        assertFalse(experiment.canSetExposureType());
    }

    @Test
    public void testCanSetExposureTypeFalseWhenWithin() {
        Experiment experiment = Experiment.builder()
            .exposureType(ExposureTypes.WITHIN)
            .build();

        assertFalse(experiment.canSetExposureType());
    }

    @Test
    public void testCanSetExposureTypeFalseWhenNull() {
        Experiment experiment = Experiment.builder()
            .exposureType(null)
            .build();

        assertFalse(experiment.canSetExposureType());
    }

    @Test
    public void testIsSingleConditionTrueWithOneCondition() {
        Experiment experiment = Experiment.builder()
            .conditions(List.of(Condition.builder().build()))
            .build();

        assertTrue(experiment.isSingleCondition());
    }

    @Test
    public void testIsSingleConditionFalseWithTwoConditions() {
        Experiment experiment = Experiment.builder()
            .conditions(List.of(Condition.builder().build(), Condition.builder().build()))
            .build();

        assertFalse(experiment.isSingleCondition());
    }

    @Test
    public void testIsSingleConditionFalseWithEmptyList() {
        Experiment experiment = Experiment.builder()
            .conditions(List.of())
            .build();

        assertFalse(experiment.isSingleCondition());
    }

    /**
     * Documents current behavior: {@code isSingleCondition()} calls {@code conditions.size()}
     * with no null-check, so a never-set (null) conditions list throws an NPE rather than
     * returning false. This is not being treated as a bug to fix here, just documented.
     */
    @Test
    public void testIsSingleConditionThrowsNpeWhenConditionsNull() {
        Experiment experiment = Experiment.builder()
            .build();

        assertThrows(NullPointerException.class, experiment::isSingleCondition);
    }

}
