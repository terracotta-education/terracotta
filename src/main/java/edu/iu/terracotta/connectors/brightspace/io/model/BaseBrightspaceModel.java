package edu.iu.terracotta.connectors.brightspace.io.model;

import lombok.extern.slf4j.Slf4j;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.json.JsonMapper;

@Slf4j
@SuppressWarnings("PMD.GuardLogStatement")
public class BaseBrightspaceModel {

    public String toJson() {
        return toJson(false);
    }

    public String toJson(boolean serializeNulls) {
        JsonMapper jsonMapper = JsonMapper.builder()
            .configure(MapperFeature.APPLY_DEFAULT_VALUES, serializeNulls)
            .configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, false)
            .build();

        try {
            return jsonMapper.writeValueAsString(this);
        } catch (JacksonException e) {
            log.error("Error serializing object to JSON", e);
            return "{}";
        }
    }

}
