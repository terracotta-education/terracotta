package edu.iu.terracotta.connectors.generic.dao.model.lms;

import edu.iu.terracotta.connectors.generic.dao.model.lms.base.BaseLmsSubmission;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
public class LmsSubmission implements BaseLmsSubmission {

    protected Class<?> type;
    protected Double score;
    protected Object user;
    protected Long userId;
    protected String userLoginId;
    protected String userName;
    protected Long attempt;

    @Override
    public LmsSubmission convert() {
        return this;
    }

}
