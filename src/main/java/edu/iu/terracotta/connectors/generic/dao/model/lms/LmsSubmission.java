package edu.iu.terracotta.connectors.generic.dao.model.lms;

import edu.iu.terracotta.connectors.generic.dao.model.lms.base.BaseLmsSubmission;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
public class LmsSubmission implements BaseLmsSubmission {

    private Class<?> type;
    private Double score;
    private Object user;
    private Long userId;
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
