package edu.iu.terracotta.connectors.caliper.dao.model.events;

import edu.iu.terracotta.connectors.caliper.dao.model.enums.Action;
import edu.iu.terracotta.dao.model.dto.media.MediaEventDto;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MediaEventTest {

    @Test
    void testCheckActionAllowsTheActionsTheVideoPlayerSends() {
        for (String action : new String[] {"Started", "Ended", "Paused", "Resumed", "Restarted", "JumpedTo", "ChangedResolution", "ChangedSpeed", "EnteredFullScreen", "ExitedFullScreen", "Muted", "Unmuted"}) {
            Action parsed = JsonMapper.builder().build().readValue("\"" + action + "\"", Action.class);

            assertDoesNotThrow(() -> MediaEvent.checkAction(parsed), action);
        }
    }

    @Test
    void testCheckActionRejectsAMissingAction() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> MediaEvent.checkAction(null));

        assertEquals("an action must be specified", exception.getMessage());
    }

    @Test
    void testCheckActionRejectsAnActionMediaEventsDoNotSupport() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> MediaEvent.checkAction(Action.SUBMITTED));

        assertEquals("SUBMITTED action is not supported", exception.getMessage());
    }

    @Test
    void testTheVideoPlayersPayloadDeserializes() {
        // as YoutubeEventCapture sends it, through media-events.module.js
        String payload = """
            {
              "type": "MediaEvent",
              "profile": "MediaProfile",
              "action": "Paused",
              "object": {"id": "https://www.youtube.com/watch?v=1", "type": "VideoObject", "mediaType": "video/vnd.youtube.yt", "duration": "PT2M"},
              "target": {"id": "https://www.youtube.com/embed/1", "type": "MediaLocation", "currentTime": "PT30S"},
              "eventTime": "2026-10-01T12:05:00.000Z",
              "extensions": {"playback_rate": 1.5}
            }
            """;

        MediaEventDto mediaEventDto = JsonMapper.builder().build().readValue(payload, MediaEventDto.class);

        assertEquals(Action.PAUSED, mediaEventDto.getAction());
        assertEquals("VideoObject", mediaEventDto.getObject().getType().value());
        assertEquals("MediaLocation", mediaEventDto.getTarget().getType().value());
        assertEquals("PT30S", mediaEventDto.getTarget().getCurrentTime());
        assertEquals(Instant.parse("2026-10-01T12:05:00Z"), mediaEventDto.getEventTime().toInstant());
        assertEquals(1.5, mediaEventDto.getExtensions().get("playback_rate"));
    }

}
