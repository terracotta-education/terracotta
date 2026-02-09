package edu.iu.terracotta.connectors.generic.dao.model.lms;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import edu.iu.terracotta.connectors.generic.dao.model.lms.base.BaseLmsSubmission;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
@JsonIgnoreProperties(ignoreUnknown = true)
public class LmsSubmission implements BaseLmsSubmission {

    private Class<?> type;
    private Double score;
    private Object user;
    private String userId;
    private String userLoginId;
    private String userName;
    private Long attempt;
    private String assignmentId;
    private boolean gradeMatchesCurrentSubmission;
    private String state;

    @Override
    public LmsSubmission from() {
        return this;
    }

}
