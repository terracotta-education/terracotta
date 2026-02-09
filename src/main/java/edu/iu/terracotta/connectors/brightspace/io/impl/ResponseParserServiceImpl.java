package edu.iu.terracotta.connectors.brightspace.io.impl;

import edu.iu.terracotta.connectors.brightspace.io.interfaces.ResponseParserService;
import edu.iu.terracotta.connectors.brightspace.io.net.model.Response;
import io.micrometer.common.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonInclude.Include;

@Slf4j
@SuppressWarnings({"PMD.GuardLogStatement", "PMD.LooseCoupling"})
public class ResponseParserServiceImpl implements ResponseParserService {

    @Override
    public <T> List<T> parseToList(TypeReference<List<T>> typeReference, Response response) {
        if (StringUtils.isBlank(response.getContent())) {
            return List.of();
        }

        JsonMapper jsonMapper = getJsonParser(false);

        try {
            return jsonMapper.readValue(response.getContent(), typeReference);
        } catch (Exception e) {
            log.error("Error parsing response to list of type [{}]", typeReference.getType(), e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public <T> Optional<T> parseToObject(Class<T> clazz, Response response) {
        if (StringUtils.isBlank(response.getContent())) {
            return Optional.empty();
        }

        JsonMapper jsonMapper = getJsonParser(false);

        try {
            return Optional.of(
                jsonMapper.readValue(
                    response.getContent(),
                    clazz
                )
            );
        } catch (Exception e) {
            log.error("Error parsing response to object of type [{}]: {}", clazz, e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public <T> Map<String, T> parseToMap(Class<T> clazz, Response response) {
        if (StringUtils.isBlank(response.getContent())) {
            return Map.of();
        }

        JsonMapper jsonMapper = getJsonParser(false);

        try {
            return jsonMapper.readValue(response.getContent(), new TypeReference<Map<String, T>>() {});
        } catch (Exception e) {
            log.error("Error parsing response to map of type [{}]: {}", clazz, e);
            throw new RuntimeException(e);
        }
    }

    public static JsonMapper getJsonParser(boolean serializeNulls) {
        return JsonMapper.builder()
            .configure(MapperFeature.DEFAULT_VIEW_INCLUSION, serializeNulls)
            .changeDefaultPropertyInclusion(incl -> serializeNulls ? incl.withValueInclusion(Include.ALWAYS) : incl.withValueInclusion(Include.NON_NULL))
            .build();
    }

}
