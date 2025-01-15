package edu.iu.terracotta.dao.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import edu.iu.terracotta.dao.model.dto.integrations.IntegrationDto;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class QuestionDto {

    private Long questionId;
    private String html;
    private Float points;
    private Long assessmentId;
    private Integer questionOrder;
    private String questionType;
    private List<AnswerDto> answers;
    private boolean randomizeAnswers; // only applies to 'MC' questions
    private IntegrationDto integration; // only for "external integration"; only for response data
    private UUID integrationClientId; // only for creating a new question with "external integration" type

}
