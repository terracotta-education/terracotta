package edu.iu.terracotta.service.app;

import edu.iu.terracotta.dao.entity.Assessment;
import edu.iu.terracotta.dao.entity.RegradeDetails;
import edu.iu.terracotta.dao.entity.Submission;
import edu.iu.terracotta.exceptions.DataServiceException;

public interface AssessmentSubmissionService {

    Submission gradeSubmission(Submission submission, RegradeDetails regradeDetails) throws DataServiceException;
    boolean isGradeAltered(Submission submission);
    Float calculateMaxScore(Assessment assessment);

}
