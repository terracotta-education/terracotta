package edu.iu.terracotta.service.app.impl;

import edu.iu.terracotta.connectors.generic.dao.model.SecuredInfo;
import edu.iu.terracotta.connectors.generic.service.api.ApiJwtService;
import edu.iu.terracotta.dao.entity.Participant;
import edu.iu.terracotta.dao.entity.Submission;
import edu.iu.terracotta.dao.model.dto.media.MediaEventDto;
import edu.iu.terracotta.exceptions.NoSubmissionsException;
import edu.iu.terracotta.exceptions.ParameterMissingException;
import edu.iu.terracotta.service.app.MediaService;
import edu.iu.terracotta.service.app.SubmissionService;
import edu.iu.terracotta.service.caliper.impl.CaliperServiceImpl;
import lombok.RequiredArgsConstructor;

import java.sql.Timestamp;
import java.time.Instant;

import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MediaServiceImpl implements MediaService {

    public static final String DATA_VERSION = "http://purl.imsglobal.org/ctx/caliper/v1p2";

    private final SubmissionService submissionService;
    private final ApiJwtService apijwtService;
    private final CaliperServiceImpl caliperService;

    @Override
    public void fromDto(MediaEventDto mediaEventDto, SecuredInfo securedInfo, Long experimentId, Long submissionId, Long questionId)
            throws ParameterMissingException, NoSubmissionsException {
        if (mediaEventDto.getEventTime() == null) {
            mediaEventDto.setEventTime(new Timestamp(Instant.now().toEpochMilli()));
        }

        if (mediaEventDto.getAction() == null) {
            throw new ParameterMissingException("MediaEvent Action not found");
        }


        if (mediaEventDto.getObject() == null) {
            throw new ParameterMissingException("MediaEvent Object not found");
        }

        boolean student = !apijwtService.isInstructorOrHigher(securedInfo);
        Submission submission = submissionService.getSubmission(experimentId, securedInfo.getUserId(), submissionId, student);
        Participant participant = submission.getParticipant();
        caliperService.sendMediaEvent(mediaEventDto, participant, securedInfo, submission, questionId);
    }

}
