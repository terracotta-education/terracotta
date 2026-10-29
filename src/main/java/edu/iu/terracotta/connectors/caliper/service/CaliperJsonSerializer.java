package edu.iu.terracotta.connectors.caliper.service;

import com.fasterxml.jackson.annotation.JsonInclude;

import edu.iu.terracotta.connectors.caliper.dao.model.Envelope;
import lombok.experimental.UtilityClass;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.ser.std.StdSerializer;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * Writes Caliper envelopes as JSON the way caliper-java 1.1.2 did: empty values are left out, including inside
 * extension and parameter maps, and times are UTC with milliseconds, as in 2026-10-01T12:00:00.000Z.
 */
@UtilityClass
public class CaliperJsonSerializer {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX").withZone(ZoneOffset.UTC);

    private static final JsonMapper JSON_MAPPER = JsonMapper.builder()
        .changeDefaultPropertyInclusion(inclusion -> JsonInclude.Value.construct(JsonInclude.Include.NON_EMPTY, JsonInclude.Include.NON_EMPTY))
        .addModule(new SimpleModule().addSerializer(Instant.class, new InstantSerializer()))
        .build();

    public String serialize(Envelope envelope) {
        return JSON_MAPPER.writeValueAsString(envelope);
    }

    private static class InstantSerializer extends StdSerializer<Instant> {

        InstantSerializer() {
            super(Instant.class);
        }

        @Override
        public void serialize(Instant value, JsonGenerator generator, SerializationContext context) {
            generator.writeString(TIME_FORMAT.format(value));
        }

    }

}
