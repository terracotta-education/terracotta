package edu.iu.terracotta.model.app.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.sql.Timestamp;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ParticipantDto {

    private Long participantId;
    private Long experimentId;
    private UserDto user;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Timestamp createdAt;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Timestamp updatedAt;
    private Boolean consent;
    private Timestamp dateGiven;
    private Timestamp dateRevoked;
    private String source;
    private Boolean dropped;
    private Long groupId;
    private boolean started;


    public ParticipantDto() {}

    public Long getParticipantId() {
        return participantId;
    }

    public void setParticipantId(Long participantId) {
        this.participantId = participantId;
    }

    public Long getExperimentId() {
        return experimentId;
    }

    public void setExperimentId(Long experimentId) {
        this.experimentId = experimentId;
    }

    public UserDto getUser() {
        return user;
    }

    public void setUser(UserDto user) {
        this.user = user;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Boolean getConsent() {return consent;}

    public void setConsent(Boolean consent) { this.consent = consent;}

    public Timestamp getDateGiven() { return dateGiven; }

    public void setDateGiven(Timestamp dateGiven) { this.dateGiven = dateGiven; }

    public Timestamp getDateRevoked() { return dateRevoked; }

    public void setDateRevoked(Timestamp dateRevoked)  { this.dateRevoked = dateRevoked; }

    public String getSource() { return source; }

    public void setSource(String source) { this.source = source; }

    public Boolean getDropped() {
        return dropped;
    }

    public void setDropped(Boolean dropped) {
        this.dropped = dropped;
    }

    public Long getGroupId() {
        return groupId;
    }

    public void setGroupId(Long groupId) {
        this.groupId = groupId;
    }

    public boolean getStarted() {
        return started;
    }

    public void setStarted(boolean started) {
        this.started = started;
    }
}
