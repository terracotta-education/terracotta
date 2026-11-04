package edu.iu.terracotta.connectors.caliper.service;

import edu.iu.terracotta.connectors.caliper.dao.model.Envelope;
import edu.iu.terracotta.connectors.caliper.dao.model.entities.Assessment;
import edu.iu.terracotta.connectors.caliper.dao.model.entities.LtiSession;
import edu.iu.terracotta.connectors.caliper.dao.model.entities.Person;
import edu.iu.terracotta.connectors.caliper.dao.model.entities.Result;
import edu.iu.terracotta.connectors.caliper.dao.model.entities.SoftwareApplication;
import edu.iu.terracotta.connectors.caliper.dao.model.enums.Action;
import edu.iu.terracotta.connectors.caliper.dao.model.enums.EntityType;
import edu.iu.terracotta.connectors.caliper.dao.model.events.AssessmentEvent;
import edu.iu.terracotta.connectors.caliper.dao.model.events.CaliperEvent;
import edu.iu.terracotta.connectors.caliper.dao.model.events.ToolUseEvent;
import edu.iu.terracotta.connectors.caliper.dao.model.events.ViewEvent;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CaliperJsonSerializerTest {

    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    @Test
    void testTimesAreUtcToTheMillisecond() {
        JsonNode json = serialize(ToolUseEvent.builder()
            .id("urn:uuid:1")
            .action(Action.USED)
            .eventTime(Instant.parse("2026-10-01T12:00:00Z"))
            .build(), Instant.parse("2026-10-01T12:00:00.123456Z"));

        assertEquals("2026-10-01T12:00:00.123Z", json.get("sendTime").asString());
        assertEquals("2026-10-01T12:00:00.000Z", json.get("data").get(0).get("eventTime").asString());
    }

    @Test
    void testEnumsAreWrittenAsTheirCaliperValues() {
        JsonNode event = serialize(ToolUseEvent.builder().id("urn:uuid:1").action(Action.USED).build()).get("data").get(0);

        assertEquals("ToolUseEvent", event.get("type").asString());
        assertEquals("Used", event.get("action").asString());
    }

    @Test
    void testEntitiesGetTheirOwnTypeUnlessGivenAnother() {
        JsonNode event = serialize(ToolUseEvent.builder()
            .id("urn:uuid:1")
            .action(Action.USED)
            .actor(Person.builder().id("person").build())
            .object(SoftwareApplication.builder().id("app").type(EntityType.AGENT).build())
            .build()).get("data").get(0);

        assertEquals("Person", event.get("actor").get("type").asString());
        assertEquals("Agent", event.get("object").get("type").asString());
    }

    @Test
    void testEmptyValuesAreLeftOut() {
        Map<String, Object> messageParameters = new HashMap<>();
        messageParameters.put("lms_course_id", "101");
        messageParameters.put("lms_assignment_id", null);
        messageParameters.put("lms_roles", new ArrayList<>());

        JsonNode envelope = serialize(AssessmentEvent.builder()
            .id("urn:uuid:1")
            .action(Action.STARTED)
            .object(Assessment.builder().id("assessment").name("").maxAttempts(0).maxScore(0).build())
            .federatedSession(LtiSession.builder().id("session").messageParameters(messageParameters).build())
            .extensions(new HashMap<>())
            .build(), "");
        JsonNode event = envelope.get("data").get(0);

        assertFalse(envelope.has("sensor"));
        assertFalse(event.has("extensions"));
        assertFalse(event.has("actor"));
        assertEquals(List.of("id", "type"), fieldNames(event.get("object")));
        assertEquals(List.of("lms_course_id"), fieldNames(event.get("federatedSession").get("messageParameters")));
    }

    @Test
    void testScoresAreWrittenAsDoubles() {
        JsonNode object = serialize(AssessmentEvent.builder()
            .id("urn:uuid:1")
            .action(Action.STARTED)
            .object(Assessment.builder().id("assessment").maxAttempts(3).maxScore(10.0F).build())
            .build()).get("data").get(0).get("object");

        assertEquals(3, object.get("maxAttempts").asInt());
        assertTrue(object.get("maxScore").isDouble());
        assertEquals("10.0", object.get("maxScore").toString());
    }

    @Test
    void testViewEventsLeaveOutTheirObject() {
        JsonNode event = serialize(ViewEvent.builder()
            .id("urn:uuid:1")
            .action(Action.VIEWED)
            .object(Result.builder().id("result").resultScore(7.25).build())
            .build()).get("data").get(0);

        assertEquals("ViewEvent", event.get("type").asString());
        assertFalse(event.has("object"));
    }

    private JsonNode serialize(CaliperEvent event) {
        return serialize(event, "sensor-1", Instant.parse("2026-10-01T12:00:00Z"));
    }

    private JsonNode serialize(CaliperEvent event, String sensorId) {
        return serialize(event, sensorId, Instant.parse("2026-10-01T12:00:00Z"));
    }

    private JsonNode serialize(CaliperEvent event, Instant sendTime) {
        return serialize(event, "sensor-1", sendTime);
    }

    private JsonNode serialize(CaliperEvent event, String sensorId, Instant sendTime) {
        return MAPPER.readTree(CaliperJsonSerializer.serialize(new Envelope(sensorId, sendTime, "http://purl.imsglobal.org/ctx/caliper/v1p2", List.of(event))));
    }

    private List<String> fieldNames(JsonNode node) {
        return new ArrayList<>(node.propertyNames());
    }

}
