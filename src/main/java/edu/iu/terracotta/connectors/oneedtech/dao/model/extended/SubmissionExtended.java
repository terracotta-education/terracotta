package edu.iu.terracotta.connectors.oneedtech.dao.model.extended;

import edu.iu.terracotta.connectors.generic.dao.model.lms.LmsSubmission;
import edu.iu.terracotta.connectors.oneedtech.dao.model.lms.Assignment;
import edu.iu.terracotta.connectors.oneedtech.dao.model.lms.Submission;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
public class SubmissionExtended extends LmsSubmission {

    @Builder.Default private Submission submission = Submission.builder().build();

    @Override
    public LmsSubmission from() {
        LmsSubmission convertedEntity = (LmsSubmission) this;
        convertedEntity.setType(Assignment.class);

        return convertedEntity;
    }

}
