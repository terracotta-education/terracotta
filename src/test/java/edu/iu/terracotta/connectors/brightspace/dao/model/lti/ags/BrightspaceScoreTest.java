package edu.iu.terracotta.connectors.brightspace.dao.model.lti.ags;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.Test;

public class BrightspaceScoreTest {

    @Test
    public void testGettersAndSetters() {
        BrightspaceScore brightspaceScore = new BrightspaceScore();

        brightspaceScore.setUserId("user-1");
        brightspaceScore.setScoreMaximum("100");
        brightspaceScore.setScoreGiven("87");
        brightspaceScore.setComment("well done");
        brightspaceScore.setActivityProgress("Completed");
        brightspaceScore.setGradingProgress("FullyGraded");
        brightspaceScore.setTimestamp("2026-09-23T00:00:00Z");
        brightspaceScore.setLmsSubmissionExtension(Map.of("key", "value"));

        assertEquals("user-1", brightspaceScore.getUserId());
        assertEquals("100", brightspaceScore.getScoreMaximum());
        assertEquals("87", brightspaceScore.getScoreGiven());
        assertEquals("well done", brightspaceScore.getComment());
        assertEquals("Completed", brightspaceScore.getActivityProgress());
        assertEquals("FullyGraded", brightspaceScore.getGradingProgress());
        assertEquals("2026-09-23T00:00:00Z", brightspaceScore.getTimestamp());
        assertEquals("value", brightspaceScore.getLmsSubmissionExtension().get("key"));
    }

    @Test
    public void testLmsSubmissionExtensionDefaultsToEmptyMap() {
        BrightspaceScore brightspaceScore = new BrightspaceScore();

        assertTrue(brightspaceScore.getLmsSubmissionExtension().isEmpty());
    }

}
