package edu.iu.terracotta.service.app.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.time.Instant;

import org.imsglobal.caliper.actions.Action;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import edu.iu.terracotta.base.BaseTest;
import edu.iu.terracotta.dao.model.dto.media.MediaEventDto;
import edu.iu.terracotta.dao.model.dto.media.MediaObjectDto;
import edu.iu.terracotta.exceptions.NoSubmissionsException;
import edu.iu.terracotta.exceptions.ParameterMissingException;
import edu.iu.terracotta.service.caliper.impl.CaliperServiceImpl;

public class MediaServiceImplTest extends BaseTest {

    // constructed manually (not @InjectMocks): ApiJwtService has multiple mock candidates
    // in the base hierarchy (apiJwtService, canvasApiJwtService), which makes constructor
    // injection ambiguous since MediaServiceImpl's parameter name doesn't exactly match either.
    private MediaServiceImpl mediaService;

    @Mock private CaliperServiceImpl caliperServiceImpl;
    @Mock private MediaEventDto mediaEventDto;
    @Mock private MediaObjectDto mediaObjectDto;

    private static final long EXPERIMENT_ID = 1L;
    private static final long SUBMISSION_ID = 1L;
    private static final long QUESTION_ID = 1L;

    @BeforeEach
    public void beforeEach() throws NoSubmissionsException {
        MockitoAnnotations.openMocks(this);

        setup();

        mediaService = new MediaServiceImpl(submissionService, apiJwtService, caliperServiceImpl);

        when(mediaEventDto.getAction()).thenReturn(Action.PAUSED);
        when(mediaEventDto.getObject()).thenReturn(mediaObjectDto);
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(false);
        when(submissionService.getSubmission(anyLong(), anyString(), anyLong(), anyBoolean())).thenReturn(submission);
    }

    @Test
    public void testFromDto() throws ParameterMissingException, NoSubmissionsException {
        Timestamp eventTime = Timestamp.from(Instant.now());
        when(mediaEventDto.getEventTime()).thenReturn(eventTime);

        mediaService.fromDto(mediaEventDto, securedInfo, EXPERIMENT_ID, SUBMISSION_ID, QUESTION_ID);

        verify(mediaEventDto, never()).setEventTime(any(Timestamp.class));
        verify(submissionService).getSubmission(EXPERIMENT_ID, securedInfo.getUserId(), SUBMISSION_ID, true);
        verify(caliperServiceImpl).sendMediaEvent(mediaEventDto, participant, securedInfo, submission, QUESTION_ID);
    }

    @Test
    public void testFromDtoSetsEventTimeWhenMissing() throws ParameterMissingException, NoSubmissionsException {
        when(mediaEventDto.getEventTime()).thenReturn(null);

        mediaService.fromDto(mediaEventDto, securedInfo, EXPERIMENT_ID, SUBMISSION_ID, QUESTION_ID);

        verify(mediaEventDto).setEventTime(any(Timestamp.class));
        verify(caliperServiceImpl).sendMediaEvent(eq(mediaEventDto), eq(participant), eq(securedInfo), eq(submission), eq(QUESTION_ID));
    }

    @Test
    public void testFromDtoStudentUser() throws ParameterMissingException, NoSubmissionsException {
        when(mediaEventDto.getEventTime()).thenReturn(Timestamp.from(Instant.now()));
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(true);

        mediaService.fromDto(mediaEventDto, securedInfo, EXPERIMENT_ID, SUBMISSION_ID, QUESTION_ID);

        verify(submissionService).getSubmission(EXPERIMENT_ID, securedInfo.getUserId(), SUBMISSION_ID, false);
    }

    @Test
    public void testFromDtoMissingAction() {
        when(mediaEventDto.getEventTime()).thenReturn(Timestamp.from(Instant.now()));
        when(mediaEventDto.getAction()).thenReturn(null);

        Exception exception = assertThrows(ParameterMissingException.class, () -> mediaService.fromDto(mediaEventDto, securedInfo, EXPERIMENT_ID, SUBMISSION_ID, QUESTION_ID));

        assertEquals("MediaEvent Action not found", exception.getMessage());
        verify(caliperServiceImpl, never()).sendMediaEvent(any(), any(), any(), any(), any());
    }

    @Test
    public void testFromDtoMissingObject() {
        when(mediaEventDto.getEventTime()).thenReturn(Timestamp.from(Instant.now()));
        when(mediaEventDto.getObject()).thenReturn(null);

        Exception exception = assertThrows(ParameterMissingException.class, () -> mediaService.fromDto(mediaEventDto, securedInfo, EXPERIMENT_ID, SUBMISSION_ID, QUESTION_ID));

        assertEquals("MediaEvent Object not found", exception.getMessage());
        verify(caliperServiceImpl, never()).sendMediaEvent(any(), any(), any(), any(), any());
    }

    @Test
    public void testFromDtoNoSubmissionsFound() throws NoSubmissionsException {
        when(mediaEventDto.getEventTime()).thenReturn(Timestamp.from(Instant.now()));
        when(submissionService.getSubmission(anyLong(), anyString(), anyLong(), anyBoolean())).thenThrow(new NoSubmissionsException("no submissions found"));

        assertThrows(NoSubmissionsException.class, () -> mediaService.fromDto(mediaEventDto, securedInfo, EXPERIMENT_ID, SUBMISSION_ID, QUESTION_ID));
    }

}
