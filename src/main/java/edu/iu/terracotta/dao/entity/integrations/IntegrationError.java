package edu.iu.terracotta.dao.entity.integrations;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@SuppressWarnings({"PMD.GuardLogStatement"})
public class IntegrationError {

    private String code;
    private boolean moreAttemptsAvailable;

    public static IntegrationError from(String json) {
        try {
            return new ObjectMapper().readValue(json, IntegrationError.class);
        } catch (JsonProcessingException e) {
            log.error("Error parsing IntegrationError from JSON: [{}]", json, e);
            return null;
        }
    }

}
