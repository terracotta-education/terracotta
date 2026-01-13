package edu.iu.terracotta.dao.entity.integrations;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

@Slf4j
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@SuppressWarnings({"PMD.GuardLogStatement"})
public class IntegrationError {

    private String code;
    private String errorMessage;
    private boolean moreAttemptsAvailable;

    public static IntegrationError from(String json) {
        try {
            return JsonMapper.builder()
                .build()
                .readValue(
                    json,
                    IntegrationError.class
                );
        } catch (JacksonException e) {
            log.error("Error parsing IntegrationError from JSON: [{}]", json, e);
            return null;
        }
    }

}
