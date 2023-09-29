package edu.iu.terracotta.service.app;

import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.model.app.Assessment;
import edu.iu.terracotta.model.app.RegradeDetails;
import edu.iu.terracotta.model.app.Submission;

public interface AssessmentSubmissionService {

    Submission gradeSubmission(Submission submission, RegradeDetails regradeDetails) throws DataServiceException;

    boolean isGradeAltered(Submission submission);

    Float calculateMaxScore(Assessment assessment);

}
