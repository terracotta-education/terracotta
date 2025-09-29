package edu.iu.terracotta.dao.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.sql.Timestamp;
import java.util.UUID;

@Getter
@Setter
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class ParticipantDto {

    private Long participantId;
    private UUID id;
    private Long experimentId;
    private UserDto user;
    private Boolean consent;
    private Timestamp dateGiven;
    private Timestamp dateRevoked;
    private String source;
    private Boolean dropped;
    private Long groupId;
    private boolean started;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Timestamp createdAt;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Timestamp updatedAt;

}
