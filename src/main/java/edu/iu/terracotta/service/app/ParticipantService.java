package edu.iu.terracotta.service.app;

import edu.iu.terracotta.exceptions.AssignmentNotMatchingException;
import edu.iu.terracotta.exceptions.ConnectionException;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.exceptions.ExperimentNotMatchingException;
import edu.iu.terracotta.exceptions.GroupNotMatchingException;
import edu.iu.terracotta.exceptions.IdInPostException;
import edu.iu.terracotta.exceptions.InvalidUserException;
import edu.iu.terracotta.exceptions.ParticipantAlreadyStartedException;
import edu.iu.terracotta.exceptions.ParticipantNotMatchingException;
import edu.iu.terracotta.exceptions.ParticipantNotUpdatedException;
import edu.iu.terracotta.model.LtiUserEntity;
import edu.iu.terracotta.model.app.Experiment;
import edu.iu.terracotta.model.app.Participant;
import edu.iu.terracotta.model.app.dto.ParticipantDto;
import edu.iu.terracotta.model.oauth2.SecuredInfo;
import org.springframework.http.HttpHeaders;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Map;

public interface ParticipantService {

    List<Participant> findAllByExperimentId(long experimentId);
    List<ParticipantDto> getParticipants(List<Participant> participants, long experimentId, String userId, boolean student, SecuredInfo securedInfo);
    Participant getParticipant(long id, long experimentId, String userId, boolean student) throws InvalidUserException;
    ParticipantDto postParticipant(ParticipantDto participantDto, long experimentId, SecuredInfo securedInfo) throws IdInPostException, DataServiceException;
    ParticipantDto toDto(Participant participant, SecuredInfo securedInfo);
    ParticipantDto toDto(Participant participant, List<Long> publishedExperimentAssignmentIds, SecuredInfo securedInfo);
    Participant fromDto(ParticipantDto participantDto) throws DataServiceException;
    void saveAndFlush(Participant participantToChange);
    List<Participant> refreshParticipants(long experimentId, List<Participant> currentParticipantList) throws ParticipantNotUpdatedException, ExperimentNotMatchingException;
    void prepareParticipation(Long experimentId, SecuredInfo securedInfo) throws ParticipantNotUpdatedException, ExperimentNotMatchingException;
    void changeParticipant(Map<Participant, ParticipantDto> map, Long experimentId, SecuredInfo securedInfo);
    Participant findParticipant(List<Participant> participants, String userId);
    HttpHeaders buildHeaders(UriComponentsBuilder ucBuilder, long experimentId, long participantId);
    void setAllToNull(Long experimentId, SecuredInfo securedInfo) throws ParticipantNotUpdatedException, ExperimentNotMatchingException;
    void setAllToTrue(Long experimentId, SecuredInfo securedInfo) throws ParticipantNotUpdatedException, ExperimentNotMatchingException;
    void setAllToFalse(Long experimentId, SecuredInfo securedInfo) throws ParticipantNotUpdatedException, ExperimentNotMatchingException;
    boolean changeConsent(ParticipantDto participantDto, SecuredInfo securedInfo, Long experimentId) throws ParticipantAlreadyStartedException, ExperimentNotMatchingException;
    void postConsentSubmission(Participant participant, SecuredInfo securedInfo) throws ConnectionException, DataServiceException;
    Participant handleExperimentParticipant(Experiment experiment, SecuredInfo securedInfo) throws GroupNotMatchingException, ParticipantNotMatchingException, ParticipantNotUpdatedException, AssignmentNotMatchingException, ExperimentNotMatchingException;
    List<Long> calculatedPublishedAssignmentIds(long experimentId, String canvasCourseId, LtiUserEntity createdBy);

}
