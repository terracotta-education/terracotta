package edu.iu.terracotta.dao.model.distribute.export;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import edu.iu.terracotta.dao.model.enums.QuestionTypes;
import edu.iu.terracotta.dao.model.enums.RegradeOption;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class QuestionExport {

    private long id;
    private Float points;
    private Integer questionOrder;
    private String html;
    private QuestionTypes questionType;
    private RegradeOption regradeOption;
    private boolean randomizeAnswers;
    private long assessmentId;
    private Long integrationId;

}
