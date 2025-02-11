package edu.iu.terracotta.connectors.generic.dao.model.lms;

import java.util.Date;
import java.util.List;

import edu.iu.terracotta.connectors.generic.dao.model.lms.base.BaseLmsAssignment;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
public class LmsAssignment implements BaseLmsAssignment {

    @Builder.Default protected int allowedAttempts = -1;
    @Builder.Default protected boolean canSubmit = true;

    protected Class<?> type;
    protected String id;
    protected String name;
    protected boolean published;
    protected String secureParams;
    protected Date dueAt;
    protected List<String> submissionTypes;
    protected Float pointsPossible;
    protected Date lockAt;
    protected Date unlockAt;

    @Override
    public LmsAssignment convert() {
        return this;
    }

}
