package edu.iu.terracotta.model.app.dto;

public class OutcomeScoreDto {

    private Long outcomeScoreId;
    private Long outcomeId;
    private Long participantId;
    private Float scoreNumeric;

    public Long getOutcomeScoreId() { return outcomeScoreId; }

    public void setOutcomeScoreId(Long outcomeScoreId) { this.outcomeScoreId = outcomeScoreId; }

    public Long getOutcomeId() { return outcomeId; }

    public void setOutcomeId(Long outcomeId) { this.outcomeId = outcomeId; }

    public Long getParticipantId() { return participantId; }

    public void setParticipantId(Long participantId) { this.participantId = participantId; }

    public Float getScoreNumeric() { return scoreNumeric; }

    public void setScoreNumeric(Float scoreNumeric) { this.scoreNumeric = scoreNumeric; }
}
