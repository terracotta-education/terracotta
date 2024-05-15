package edu.iu.terracotta.service.app;

import edu.iu.terracotta.exceptions.AssessmentNotMatchingException;
import edu.iu.terracotta.exceptions.CanvasApiException;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.exceptions.ExceedingLimitException;
import edu.iu.terracotta.exceptions.IdInPostException;
import edu.iu.terracotta.exceptions.QuestionNotMatchingException;
import edu.iu.terracotta.exceptions.TreatmentNotMatchingException;
import edu.iu.terracotta.model.LtiUserEntity;
import edu.iu.terracotta.model.app.Assignment;
import edu.iu.terracotta.model.app.Treatment;
import edu.iu.terracotta.model.app.dto.AssignmentDto;
import edu.iu.terracotta.model.app.dto.TreatmentDto;
import edu.iu.terracotta.model.oauth2.SecuredInfo;

public interface AssignmentTreatmentService {

    TreatmentDto duplicateTreatment(long treatmentId, SecuredInfo securedInfo)
        throws IdInPostException, DataServiceException, ExceedingLimitException, AssessmentNotMatchingException, NumberFormatException,
            CanvasApiException, TreatmentNotMatchingException, QuestionNotMatchingException;
    TreatmentDto duplicateTreatment(long treatmentId, Assignment assignment, SecuredInfo securedInfo)
        throws IdInPostException, DataServiceException, ExceedingLimitException, AssessmentNotMatchingException, NumberFormatException,
            CanvasApiException, TreatmentNotMatchingException, QuestionNotMatchingException;
    TreatmentDto toTreatmentDto(Treatment treatment, boolean submissions, boolean addAssignmentDto) throws AssessmentNotMatchingException;
    AssignmentDto toAssignmentDto(Assignment assignment, boolean submissions, boolean addTreatmentDto) throws AssessmentNotMatchingException;
    void setAssignmentDtoAttrs(Assignment assignment, String canvasCourseId, LtiUserEntity instructorUser) throws NumberFormatException, CanvasApiException;

}
