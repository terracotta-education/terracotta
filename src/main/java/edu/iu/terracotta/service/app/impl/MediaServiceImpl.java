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

import java.sql.Timestamp;
import java.time.Instant;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class MediaServiceImpl implements MediaService {

    public static final String DATA_VERSION = "http://purl.imsglobal.org/ctx/caliper/v1p2";

    @Autowired private SubmissionService submissionService;
    @Autowired private ApiJwtService apijwtService;
    @Autowired private CaliperServiceImpl caliperService;

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
