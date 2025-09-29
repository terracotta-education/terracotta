package edu.iu.terracotta.connectors.generic.dao.model.lms;

import java.util.Date;
import java.util.List;

import edu.iu.terracotta.connectors.generic.dao.model.lms.base.BaseLmsAssignment;
import edu.iu.terracotta.connectors.generic.dao.model.lms.base.LmsExternalToolFields;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
public class LmsAssignment implements BaseLmsAssignment {

    @Builder.Default private int allowedAttempts = -1;
    @Builder.Default private boolean canSubmit = true;

    private Class<?> type;
    private String id;
    private String name;
    private boolean published;
    private String secureParams;
    private Date dueAt;
    private List<String> submissionTypes;
    private Float pointsPossible;
    private Date lockAt;
    private Date unlockAt;
    private LmsExternalToolFields lmsExternalToolFields;
    private String gradingType;

    @Override
    public LmsAssignment from() {
        return this;
    }

}
