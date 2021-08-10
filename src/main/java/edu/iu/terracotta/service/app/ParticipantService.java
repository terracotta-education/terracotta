package edu.iu.terracotta.service.app;

import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.exceptions.IdInPostException;
import edu.iu.terracotta.exceptions.InvalidUserException;
import edu.iu.terracotta.exceptions.ParticipantNotUpdatedException;
import edu.iu.terracotta.model.app.Participant;
import edu.iu.terracotta.model.app.dto.ParticipantDto;
import edu.iu.terracotta.model.oauth2.SecuredInfo;
import org.springframework.http.HttpHeaders;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface ParticipantService {
    List<Participant> findAllByExperimentId(long experimentId);

    List<ParticipantDto> getParticipants(List<Participant> participants, long experimentId, String userId, boolean student);

    Participant getParticipant(long id, long experimentId, String userId, boolean student) throws InvalidUserException;

    ParticipantDto postParticipant(ParticipantDto participantDto, long experimentId) throws IdInPostException, DataServiceException;

    ParticipantDto toDto(Participant participant);

    Optional<Participant> findById(Long id);

    Optional<Participant> findByParticipantIdAndExperimentId(Long participantId, Long experimentId);

    Participant fromDto(ParticipantDto participantDto) throws DataServiceException;

    void saveAndFlush(Participant participantToChange);

    Participant save(Participant participant);

    void deleteById(Long id);

    List<Participant> refreshParticipants(long experimentId, SecuredInfo securedInfo, List<Participant> currentParticipantList) throws ParticipantNotUpdatedException;

    boolean participantBelongsToExperiment(Long experimentId, Long participantId);

    void prepareParticipation(Long experimentId, SecuredInfo securedInfo) throws ParticipantNotUpdatedException;

    void changeParticipant(Map<Participant, ParticipantDto> map, Long experimentId);

    Participant findParticipant(List<Participant> participants, String userId);

    HttpHeaders buildHeaders(UriComponentsBuilder ucBuilder, long experimentId, long participantId);

    void setAllToNull(Long experimentId, SecuredInfo securedInfo) throws ParticipantNotUpdatedException;

    void setAllToTrue(Long experimentId, SecuredInfo securedInfo) throws ParticipantNotUpdatedException;

    void setAllToFalse(Long experimentId, SecuredInfo securedInfo) throws ParticipantNotUpdatedException;

    boolean changeConsent(ParticipantDto participantDto, SecuredInfo securedInfo, Long experimentId);
}
