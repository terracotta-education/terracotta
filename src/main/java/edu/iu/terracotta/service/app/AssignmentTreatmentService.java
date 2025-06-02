package edu.iu.terracotta.service.app;

import java.util.List;

import edu.iu.terracotta.connectors.generic.dao.entity.lti.LtiUserEntity;
import edu.iu.terracotta.connectors.generic.dao.model.SecuredInfo;
import edu.iu.terracotta.connectors.generic.exceptions.ApiException;
import edu.iu.terracotta.connectors.generic.exceptions.TerracottaConnectorException;
import edu.iu.terracotta.dao.entity.Assignment;
import edu.iu.terracotta.dao.entity.Treatment;
import edu.iu.terracotta.dao.exceptions.AssessmentNotMatchingException;
import edu.iu.terracotta.dao.exceptions.QuestionNotMatchingException;
import edu.iu.terracotta.dao.exceptions.TreatmentNotMatchingException;
import edu.iu.terracotta.dao.model.dto.AssignmentDto;
import edu.iu.terracotta.dao.model.dto.TreatmentDto;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.exceptions.ExceedingLimitException;
import edu.iu.terracotta.exceptions.IdInPostException;

public interface AssignmentTreatmentService {

    TreatmentDto duplicateTreatment(long treatmentId, SecuredInfo securedInfo)
        throws IdInPostException, DataServiceException, ExceedingLimitException, AssessmentNotMatchingException, NumberFormatException,
            TreatmentNotMatchingException, QuestionNotMatchingException, ApiException, TerracottaConnectorException;
    TreatmentDto duplicateTreatment(long treatmentId, Assignment assignment, SecuredInfo securedInfo)
        throws IdInPostException, DataServiceException, ExceedingLimitException, AssessmentNotMatchingException, NumberFormatException,
            TreatmentNotMatchingException, QuestionNotMatchingException, ApiException, TerracottaConnectorException;
    TreatmentDto toTreatmentDto(Treatment treatment, boolean submissions, boolean addAssignmentDto) throws AssessmentNotMatchingException;
    AssignmentDto toAssignmentDto(Assignment assignment, boolean submissions, boolean addTreatmentDto) throws AssessmentNotMatchingException;
    List<AssignmentDto> toAssignmentDto(List<Assignment> assignments, boolean submissions, boolean addTreatmentDto) throws AssessmentNotMatchingException;
    void setAssignmentDtoAttrs(Assignment assignment, String lmsCourseId, LtiUserEntity instructorUser) throws NumberFormatException, ApiException, TerracottaConnectorException;

}
