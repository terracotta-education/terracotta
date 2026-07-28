package edu.iu.terracotta.service.app;

import edu.iu.terracotta.connectors.generic.dao.entity.lti.LtiUserEntity;
import edu.iu.terracotta.connectors.generic.dao.model.SecuredInfo;
import edu.iu.terracotta.connectors.generic.exceptions.ConnectionException;
import edu.iu.terracotta.connectors.generic.exceptions.TerracottaConnectorException;
import edu.iu.terracotta.dao.entity.Experiment;
import edu.iu.terracotta.dao.entity.Participant;
import edu.iu.terracotta.dao.exceptions.AssignmentNotMatchingException;
import edu.iu.terracotta.dao.exceptions.ExperimentNotMatchingException;
import edu.iu.terracotta.dao.exceptions.GroupNotMatchingException;
import edu.iu.terracotta.dao.exceptions.ParticipantNotMatchingException;
import edu.iu.terracotta.dao.exceptions.ParticipantNotUpdatedException;
import edu.iu.terracotta.dao.model.dto.ParticipantDto;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.exceptions.IdInPostException;
import edu.iu.terracotta.exceptions.InvalidUserException;
import edu.iu.terracotta.exceptions.ParticipantAlreadyStartedException;

import org.springframework.http.HttpHeaders;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Map;

@SuppressWarnings({"PMD.LooseCoupling"})
public interface ParticipantService {

    List<Participant> findAllByExperimentId(long experimentId);
    List<ParticipantDto> getParticipants(long experimentId, String userId, boolean student, SecuredInfo securedInfo, boolean refresh) throws ParticipantNotUpdatedException, ExperimentNotMatchingException, TerracottaConnectorException;
    Participant getParticipant(long id, long experimentId, String userId, boolean student) throws InvalidUserException, ParticipantNotMatchingException;
    ParticipantDto postParticipant(ParticipantDto participantDto, long experimentId, SecuredInfo securedInfo) throws IdInPostException, DataServiceException;
    ParticipantDto toDto(Participant participant, SecuredInfo securedInfo);
    ParticipantDto toDto(Participant participant, List<Long> publishedExperimentAssignmentIds, SecuredInfo securedInfo);
    Participant fromDto(ParticipantDto participantDto) throws DataServiceException;
    void saveAndFlush(Participant participantToChange);
    void refreshParticipants(long experimentId) throws ParticipantNotUpdatedException, ExperimentNotMatchingException, TerracottaConnectorException;
    void refreshParticipantsIfStale(long experimentId) throws ParticipantNotUpdatedException, ExperimentNotMatchingException, TerracottaConnectorException;
    void ensureParticipantExists(long experimentId, SecuredInfo securedInfo) throws ExperimentNotMatchingException, ParticipantNotUpdatedException, TerracottaConnectorException;
    void prepareParticipation(Long experimentId, SecuredInfo securedInfo) throws ParticipantNotUpdatedException, ExperimentNotMatchingException, TerracottaConnectorException;
    List<Participant> changeParticipant(Map<Participant, ParticipantDto> map, Long experimentId, SecuredInfo securedInfo);
    Participant findParticipant(long experimentId, String userId);
    HttpHeaders buildHeaders(UriComponentsBuilder ucBuilder, long experimentId, long participantId);
    void setAllToNull(Long experimentId) throws ParticipantNotUpdatedException, ExperimentNotMatchingException, TerracottaConnectorException;
    void setAllToTrue(Long experimentId) throws ParticipantNotUpdatedException, ExperimentNotMatchingException, TerracottaConnectorException;
    void setAllToFalse(Long experimentId) throws ParticipantNotUpdatedException, ExperimentNotMatchingException, TerracottaConnectorException;
    Participant changeConsent(ParticipantDto participantDto, SecuredInfo securedInfo, Long experimentId) throws ParticipantAlreadyStartedException, ExperimentNotMatchingException, ParticipantNotMatchingException;
    void postConsentSubmission(Participant participant, SecuredInfo securedInfo) throws ConnectionException, DataServiceException, TerracottaConnectorException;
    Participant handleExperimentParticipant(Experiment experiment, SecuredInfo securedInfo) throws GroupNotMatchingException, ParticipantNotMatchingException, ParticipantNotUpdatedException, AssignmentNotMatchingException, ExperimentNotMatchingException, TerracottaConnectorException;
    List<Long> calculatedPublishedAssignmentIds(long experimentId, String lmsCourseId, LtiUserEntity createdBy);

}
