package edu.iu.terracotta.connectors.brightspace.io.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.extern.slf4j.Slf4j;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

@Slf4j
@SuppressWarnings("PMD.GuardLogStatement")
public class BaseBrightspaceModel {

    // JsonMapper is thread-safe once built; reuse one shared instance per serializeNulls variant,
    // shared across every subclass, instead of building a fresh mapper on every toJson() call.
    private static final JsonMapper JSON_MAPPER_SERIALIZE_NULLS = buildJsonMapper(true);
    private static final JsonMapper JSON_MAPPER_OMIT_NULLS = buildJsonMapper(false);

    public String toJson() {
        return toJson(false);
    }

    public String toJson(boolean serializeNulls) {
        JsonMapper jsonMapper = serializeNulls ? JSON_MAPPER_SERIALIZE_NULLS : JSON_MAPPER_OMIT_NULLS;

        try {
            return jsonMapper.writeValueAsString(this);
        } catch (JacksonException e) {
            log.error("Error serializing object to JSON", e);
            return "{}";
        }
    }

    private static JsonMapper buildJsonMapper(boolean serializeNulls) {
        return JsonMapper.builder()
            .changeDefaultPropertyInclusion(
                incl -> incl.withValueInclusion(serializeNulls ? JsonInclude.Include.ALWAYS : JsonInclude.Include.NON_NULL)
            )
            .configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, false)
            .build();
    }

}
