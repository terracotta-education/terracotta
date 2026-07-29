package edu.iu.terracotta.service.app.impl;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.util.UriComponentsBuilder;

import edu.iu.terracotta.base.BaseTest;
import edu.iu.terracotta.connectors.generic.exceptions.ApiException;
import edu.iu.terracotta.dao.entity.Experiment;
import edu.iu.terracotta.dao.entity.Participant;
import edu.iu.terracotta.dao.model.dto.ConditionDto;
import edu.iu.terracotta.dao.model.dto.ExperimentDto;
import edu.iu.terracotta.dao.model.dto.ExposureDto;
import edu.iu.terracotta.dao.model.enums.ParticipationTypes;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.exceptions.TitleValidationException;
import edu.iu.terracotta.exceptions.WrongValueException;
import edu.iu.terracotta.service.app.ConditionService;
import edu.iu.terracotta.service.app.FeatureService;
import edu.iu.terracotta.service.app.async.AssignmentAsyncService;
import edu.iu.terracotta.service.app.async.ParticipantAsyncService;

public class ExperimentServiceImplTest extends BaseTest {

    @Mock private AssignmentAsyncService assignmentAsyncService;
    @Mock private ParticipantAsyncService participantAsyncService;
    @Mock private ConditionService conditionService;
    @Mock private FeatureService featureService;

    @InjectMocks private ExperimentServiceImpl experimentService;

    @BeforeEach
    public void beforeEach() {
        MockitoAnnotations.openMocks(this);

        setup();

        ReflectionTestUtils.setField(experimentService, "batchSize", 500);
    }

    @Test
    public void testGetExperimentsNoSync() throws Exception {
        when(experimentRepository.findByPlatformDeployment_KeyIdAndLtiContextEntity_ContextId(anyLong(), anyLong())).thenReturn(List.of(experiment));

        List<ExperimentDto> retVal = experimentService.getExperiments(securedInfo, false);

        assertEquals(1, retVal.size());
        verify(assignmentAsyncService, never()).handleAssignmentTasksInLmsByContext(any());
        verify(participantAsyncService, never()).updateParticipantData(any());
    }

    @Test
    public void testGetExperimentsWithSync() throws Exception {
        when(experimentRepository.findByPlatformDeployment_KeyIdAndLtiContextEntity_ContextId(anyLong(), anyLong())).thenReturn(List.of(experiment));

        List<ExperimentDto> retVal = experimentService.getExperiments(securedInfo, true);

        assertEquals(1, retVal.size());
        verify(assignmentAsyncService).handleAssignmentTasksInLmsByContext(securedInfo);
        verify(participantAsyncService).updateParticipantData(securedInfo);
    }

    @Test
    public void testGetExperimentsSyncSkipsParticipantUpdateWhenEmpty() throws Exception {
        when(experimentRepository.findByPlatformDeployment_KeyIdAndLtiContextEntity_ContextId(anyLong(), anyLong())).thenReturn(Collections.emptyList());

        List<ExperimentDto> retVal = experimentService.getExperiments(securedInfo, true);

        assertEquals(0, retVal.size());
        verify(assignmentAsyncService).handleAssignmentTasksInLmsByContext(securedInfo);
        verify(participantAsyncService, never()).updateParticipantData(any());
    }

    @Test
    public void testGetExperimentsSyncExceptionSwallowed() throws Exception {
        when(experimentRepository.findByPlatformDeployment_KeyIdAndLtiContextEntity_ContextId(anyLong(), anyLong())).thenReturn(List.of(experiment));
        doThrow(new ApiException("fail")).when(assignmentAsyncService).handleAssignmentTasksInLmsByContext(any());

        List<ExperimentDto> retVal = assertDoesNotThrow(() -> experimentService.getExperiments(securedInfo, true));

        assertEquals(1, retVal.size());
    }

    @Test
    public void testGetExperiment() {
        Experiment retVal = experimentService.getExperiment(1L);

        assertEquals(experiment, retVal);
    }

    @Test
    public void testPostExperimentSuccess() throws Exception {
        ExperimentDto experimentDto = ExperimentDto.builder()
            .experimentId(1L)
            .title("New Experiment")
            .exposureType("BETWEEN")
            .participationType("AUTO")
            .distributionType("EVEN")
            .build();

        when(ltiUserRepository.findFirstByUserIdAndPlatformDeployment_KeyId(anyLong(), anyLong())).thenReturn(ltiUserEntity);
        when(experimentRepository.save(any(Experiment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ExperimentDto retVal = experimentService.postExperiment(experimentDto, securedInfo);

        assertNotNull(retVal);
        assertEquals("New Experiment", retVal.getTitle());
        verify(participantAsyncService).updateParticipantData(eq(securedInfo));
    }

    @Test
    public void testPostExperimentTitleAlreadyExistsThrows() {
        ExperimentDto experimentDto = ExperimentDto.builder().title("Existing Title").build();
        when(experimentRepository.existsByTitleAndLtiContextEntity_ContextIdAndExperimentIdIsNot(anyString(), anyLong(), anyLong())).thenReturn(true);

        Exception exception = assertThrows(TitleValidationException.class, () -> experimentService.postExperiment(experimentDto, securedInfo));

        assertTrue(exception.getMessage().startsWith("Error 102"));
    }

    @Test
    public void testPostExperimentDataServiceExceptionWrapped() {
        ExperimentDto experimentDto = ExperimentDto.builder().title("Valid Title").build();
        when(ltiContextRepository.findById(anyLong())).thenReturn(Optional.empty());

        Exception exception = assertThrows(DataServiceException.class, () -> experimentService.postExperiment(experimentDto, securedInfo));

        assertTrue(exception.getMessage().startsWith("Error 105"));
    }

    @Test
    public void testPostExperimentSyncExceptionSwallowed() throws Exception {
        ExperimentDto experimentDto = ExperimentDto.builder().experimentId(1L).title("New Experiment").build();
        when(ltiUserRepository.findFirstByUserIdAndPlatformDeployment_KeyId(anyLong(), anyLong())).thenReturn(ltiUserEntity);
        when(experimentRepository.save(any(Experiment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doThrow(new ApiException("fail")).when(participantAsyncService).updateParticipantData(any());

        ExperimentDto retVal = assertDoesNotThrow(() -> experimentService.postExperiment(experimentDto, securedInfo));

        assertNotNull(retVal);
    }

    @Test
    public void testUpdateExperimentSuccess() throws Exception {
        ExperimentDto experimentDto = ExperimentDto.builder()
            .title("Updated Title")
            .exposureType("BETWEEN")
            .build();

        experimentService.updateExperiment(1L, 1L, experimentDto, securedInfo);

        verify(experiment).setTitle("Updated Title");
        verify(experiment).setDescription(null);
        verify(experimentRepository).save(experiment);
    }

    @Test
    public void testUpdateExperimentBothTitlesBlankThrows() {
        ExperimentDto experimentDto = ExperimentDto.builder().build();

        Exception exception = assertThrows(TitleValidationException.class, () -> experimentService.updateExperiment(1L, 1L, experimentDto, securedInfo));

        assertEquals("Error 100: Please give the experiment a title.", exception.getMessage());
    }

    @Test
    public void testUpdateExperimentTitleTooLongThrows() {
        ExperimentDto experimentDto = ExperimentDto.builder().title("a".repeat(256)).build();

        Exception exception = assertThrows(TitleValidationException.class, () -> experimentService.updateExperiment(1L, 1L, experimentDto, securedInfo));

        assertEquals("Error 101: Experiment title must be 255 characters or less.", exception.getMessage());
    }

    @Test
    public void testUpdateExperimentTitleAlreadyExistsThrows() {
        ExperimentDto experimentDto = ExperimentDto.builder().title("Duplicate").build();
        when(experimentRepository.existsByTitleAndLtiContextEntity_ContextIdAndExperimentIdIsNot(anyString(), anyLong(), anyLong())).thenReturn(true);

        Exception exception = assertThrows(TitleValidationException.class, () -> experimentService.updateExperiment(1L, 1L, experimentDto, securedInfo));

        assertTrue(exception.getMessage().contains("Duplicate"));
    }

    @Test
    public void testUpdateExperimentStartedExposureTypeChangeThrows() {
        when(experiment.isStarted()).thenReturn(true);
        ExperimentDto experimentDto = ExperimentDto.builder().title("Title").exposureType("WITHIN").build();

        Exception exception = assertThrows(WrongValueException.class, () -> experimentService.updateExperiment(1L, 1L, experimentDto, securedInfo));

        assertEquals("Error 110: The experiment has started. The Exposure Type can't be changed", exception.getMessage());
    }

    @Test
    public void testUpdateExperimentStartedDistributionTypeChangeThrows() {
        when(experiment.isStarted()).thenReturn(true);
        ExperimentDto experimentDto = ExperimentDto.builder().title("Title").exposureType("BETWEEN").distributionType("MANUAL").build();

        Exception exception = assertThrows(WrongValueException.class, () -> experimentService.updateExperiment(1L, 1L, experimentDto, securedInfo));

        assertEquals("Error 110: The experiment has started. The Distribution Type can't be changed", exception.getMessage());
    }

    @Test
    public void testUpdateExperimentStartedParticipationFromConsentThrows() {
        when(experiment.isStarted()).thenReturn(true);
        when(experiment.getParticipationType()).thenReturn(ParticipationTypes.CONSENT);
        ExperimentDto experimentDto = ExperimentDto.builder().title("Title").exposureType("BETWEEN").distributionType("EVEN").participationType("AUTO").build();

        Exception exception = assertThrows(WrongValueException.class, () -> experimentService.updateExperiment(1L, 1L, experimentDto, securedInfo));

        assertTrue(exception.getMessage().contains("can't be changed from 'Consent' to AUTO"));
    }

    @Test
    public void testUpdateExperimentCannotChangeExposureTypeThrows() {
        ExperimentDto experimentDto = ExperimentDto.builder().title("Title").exposureType("WITHIN").build();

        Exception exception = assertThrows(WrongValueException.class, () -> experimentService.updateExperiment(1L, 1L, experimentDto, securedInfo));

        assertEquals("Error 110: The experiment has an existing exposure type. The Exposure Type can't be changed.", exception.getMessage());
    }

    @Test
    public void testUpdateExperimentInvalidExposureTypeThrows() {
        when(experiment.canSetExposureType()).thenReturn(true);
        ExperimentDto experimentDto = ExperimentDto.builder().title("Title").exposureType("BOGUS").build();

        Exception exception = assertThrows(WrongValueException.class, () -> experimentService.updateExperiment(1L, 1L, experimentDto, securedInfo));

        assertEquals("Error 134: BOGUS is not a valid Exposure value", exception.getMessage());
    }

    @Test
    public void testUpdateExperimentInvalidDistributionTypeThrows() {
        ExperimentDto experimentDto = ExperimentDto.builder().title("Title").exposureType("BETWEEN").distributionType("BOGUS").build();

        Exception exception = assertThrows(WrongValueException.class, () -> experimentService.updateExperiment(1L, 1L, experimentDto, securedInfo));

        assertEquals("Error 134: BOGUS is not a valid Distribution value", exception.getMessage());
    }

    @Test
    public void testUpdateExperimentInvalidParticipationTypeThrows() {
        ExperimentDto experimentDto = ExperimentDto.builder().title("Title").exposureType("BETWEEN").participationType("BOGUS").build();

        Exception exception = assertThrows(WrongValueException.class, () -> experimentService.updateExperiment(1L, 1L, experimentDto, securedInfo));

        assertEquals("Error 134: BOGUS is not a valid Participation value", exception.getMessage());
    }

    @Test
    public void testUpdateExperimentParticipationChangeFromConsentDeletesConsentDocument() throws Exception {
        when(experiment.getParticipationType()).thenReturn(ParticipationTypes.CONSENT);
        ExperimentDto experimentDto = ExperimentDto.builder().title("Title").exposureType("BETWEEN").participationType("AUTO").build();

        experimentService.updateExperiment(1L, 1L, experimentDto, securedInfo);

        verify(consentDocumentRepository).delete(consentDocument);
        verify(experiment).setConsentDocument(null);
        verify(participantService).setAllToTrue(1L);
        verify(experiment).setParticipationType(ParticipationTypes.AUTO);
    }

    @Test
    public void testUpdateExperimentParticipationChangeFromConsentSwallowsDeleteFailure() throws Exception {
        when(experiment.getParticipationType()).thenReturn(ParticipationTypes.CONSENT);
        doThrow(new ApiException("fail")).when(fileStorageService).deleteConsentAssignment(anyLong(), any());
        ExperimentDto experimentDto = ExperimentDto.builder().title("Title").exposureType("BETWEEN").participationType("AUTO").build();

        experimentService.updateExperiment(1L, 1L, experimentDto, securedInfo);

        verify(consentDocumentRepository, never()).delete(any());
        verify(participantService).setAllToTrue(1L);
    }

    // MANUAL participation type has no consent process, so existing participants' consent
    // resets to null - matching buildAndSaveParticipant's mapping for brand-new participants.
    @Test
    public void testUpdateExperimentParticipationChangeToManualCallsSetAllToNull() throws Exception {
        ExperimentDto experimentDto = ExperimentDto.builder().title("Title").exposureType("BETWEEN").participationType("MANUAL").build();

        experimentService.updateExperiment(1L, 1L, experimentDto, securedInfo);

        verify(participantService).setAllToNull(1L);
        verify(experiment).setParticipationType(ParticipationTypes.MANUAL);
    }

    // CONSENT participation type starts unanswered (not yet consented), so existing
    // participants' consent resets to false - matching buildAndSaveParticipant's mapping.
    @Test
    public void testUpdateExperimentParticipationChangeToConsentCallsSetAllToFalse() throws Exception {
        ExperimentDto experimentDto = ExperimentDto.builder().title("Title").exposureType("BETWEEN").participationType("CONSENT").build();

        experimentService.updateExperiment(1L, 1L, experimentDto, securedInfo);

        verify(participantService).setAllToFalse(1L);
        verify(experiment).setParticipationType(ParticipationTypes.CONSENT);
    }

    @Test
    public void testToDtoBasicNoFlags() {
        ExperimentDto retVal = experimentService.toDto(experiment, false, false, false, securedInfo);

        assertEquals(1L, retVal.getExperimentId());
        assertEquals("BETWEEN", retVal.getExposureType());
        assertEquals("AUTO", retVal.getParticipationType());
        assertEquals("EVEN", retVal.getDistributionType());
        assertTrue(retVal.getConditions().isEmpty());
        assertTrue(retVal.getExposures().isEmpty());
        assertTrue(retVal.getParticipants().isEmpty());
        assertNotNull(retVal.getConsent());
    }

    @Test
    public void testToDtoNoConsentDocumentWhenNull() {
        when(experiment.getConsentDocument()).thenReturn(null);

        ExperimentDto retVal = experimentService.toDto(experiment, false, false, false, securedInfo);

        assertNull(retVal.getConsent());
    }

    @Test
    public void testToDtoWithConditionsExposuresAndParticipants() {
        ConditionDto conditionDto = mock(ConditionDto.class);
        ExposureDto exposureDto = mock(ExposureDto.class);
        when(conditionService.toDto(condition)).thenReturn(conditionDto);
        when(exposureService.toDto(exposure)).thenReturn(exposureDto);
        when(participantService.toDto(eq(participant), any(), eq(securedInfo))).thenReturn(participantDto);

        ExperimentDto retVal = experimentService.toDto(experiment, true, true, true, securedInfo);

        assertEquals(1, retVal.getConditions().size());
        assertEquals(1, retVal.getExposures().size());
        assertEquals(1, retVal.getParticipants().size());
    }

    @Test
    public void testToDtoFiltersTestStudentParticipants() {
        Participant testStudent = mock(Participant.class);
        when(testStudent.isTestStudent()).thenReturn(true);
        when(participantRepository.findByExperiment_ExperimentId(anyLong())).thenReturn(List.of(participant, testStudent));
        when(participantService.toDto(eq(participant), any(), eq(securedInfo))).thenReturn(participantDto);

        ExperimentDto retVal = experimentService.toDto(experiment, false, false, true, securedInfo);

        assertEquals(1, retVal.getParticipants().size());
    }

    @Test
    public void testToDtoCountsAnsweredAcceptedAndRejectedParticipants() {
        Participant rejected = mock(Participant.class);
        when(rejected.getDateRevoked()).thenReturn(Timestamp.from(Instant.now()));
        when(rejected.getConsent()).thenReturn(false);
        when(rejected.getLtiUserEntity()).thenReturn(ltiUserEntity);
        when(participantRepository.findByExperiment_ExperimentId(anyLong(), any(Pageable.class)))
            .thenReturn(List.of(participant, rejected))
            .thenReturn(Collections.emptyList());

        ExperimentDto retVal = experimentService.toDto(experiment, false, false, false, securedInfo);

        assertEquals(2, retVal.getPotentialParticipants());
        assertEquals(1, retVal.getAcceptedParticipants());
        assertEquals(1, retVal.getRejectedParticipants());
    }

    @Test
    public void testToDtoNullConsentCountsAsRejectedNotAccepted() {
        Participant nullConsent = mock(Participant.class);
        when(nullConsent.getDateRevoked()).thenReturn(Timestamp.from(Instant.now()));
        when(nullConsent.getConsent()).thenReturn(null);
        when(nullConsent.getLtiUserEntity()).thenReturn(ltiUserEntity);
        when(participantRepository.findByExperiment_ExperimentId(anyLong(), any(Pageable.class)))
            .thenReturn(List.of(nullConsent))
            .thenReturn(Collections.emptyList());

        ExperimentDto retVal = experimentService.toDto(experiment, false, false, false, securedInfo);

        assertEquals(0, retVal.getAcceptedParticipants());
        assertEquals(1, retVal.getRejectedParticipants());
    }

    @Test
    public void testToDtoNoParticipantsLeavesPotentialParticipantsNull() {
        when(participantRepository.findByExperiment_ExperimentId(anyLong(), any(Pageable.class))).thenReturn(Collections.emptyList());

        ExperimentDto retVal = experimentService.toDto(experiment, false, false, false, securedInfo);

        assertEquals(0, retVal.getPotentialParticipants());
        assertEquals(0, retVal.getAcceptedParticipants());
        assertEquals(0, retVal.getRejectedParticipants());
    }

    @Test
    public void testFromDtoSuccess() throws Exception {
        ExperimentDto experimentDto = ExperimentDto.builder()
            .contextId(1L)
            .platformDeploymentId(1L)
            .title("Title")
            .exposureType("BETWEEN")
            .participationType("AUTO")
            .distributionType("EVEN")
            .createdBy(1L)
            .build();
        when(ltiUserRepository.findFirstByUserIdAndPlatformDeployment_KeyId(anyLong(), anyLong())).thenReturn(ltiUserEntity);

        Experiment retVal = experimentService.fromDto(experimentDto);

        assertEquals("Title", retVal.getTitle());
        assertEquals(ltiContextEntity, retVal.getLtiContextEntity());
        assertEquals(platformDeployment, retVal.getPlatformDeployment());
        assertEquals(ltiUserEntity, retVal.getCreatedBy());
    }

    @Test
    public void testFromDtoContextNotFoundThrows() {
        when(ltiContextRepository.findById(anyLong())).thenReturn(Optional.empty());
        ExperimentDto experimentDto = ExperimentDto.builder().contextId(1L).build();

        Exception exception = assertThrows(DataServiceException.class, () -> experimentService.fromDto(experimentDto));

        assertEquals("The course defined in the experiment dto does not exist", exception.getMessage());
    }

    @Test
    public void testFromDtoPlatformDeploymentNotFoundThrows() {
        when(platformDeploymentRepository.findById(anyLong())).thenReturn(Optional.empty());
        ExperimentDto experimentDto = ExperimentDto.builder().contextId(1L).platformDeploymentId(1L).build();

        Exception exception = assertThrows(DataServiceException.class, () -> experimentService.fromDto(experimentDto));

        assertEquals("The platform deployment defined in the experiment dto does not exist", exception.getMessage());
    }

    @Test
    public void testFromDtoUserNotFoundThrows() {
        when(ltiUserRepository.findFirstByUserIdAndPlatformDeployment_KeyId(anyLong(), anyLong())).thenReturn(null);
        ExperimentDto experimentDto = ExperimentDto.builder().contextId(1L).platformDeploymentId(1L).createdBy(1L).build();

        Exception exception = assertThrows(DataServiceException.class, () -> experimentService.fromDto(experimentDto));

        assertEquals("The user specified to create the experiment does not exist or does not belong to this course", exception.getMessage());
    }

    @Test
    public void testDeleteById() throws Exception {
        experimentService.deleteById(1L, securedInfo);

        verify(assignmentService).deleteAllFromExperiment(1L, securedInfo);
        verify(fileStorageService).deleteConsentAssignment(1L, securedInfo);
        verify(experimentRepository).deleteByExperimentId(1L);
    }

    @Test
    public void testDeleteByIdSwallowsConsentDeleteFailure() throws Exception {
        doThrow(new ApiException("fail")).when(fileStorageService).deleteConsentAssignment(anyLong(), any());

        experimentService.deleteById(1L, securedInfo);

        verify(experimentRepository).deleteByExperimentId(1L);
    }

    @Test
    public void testFillContextInfoSetsCreatedByWhenUserFound() {
        ExperimentDto experimentDto = ExperimentDto.builder().build();

        ExperimentDto retVal = experimentService.fillContextInfo(experimentDto, securedInfo);

        assertEquals(securedInfo.getContextId(), retVal.getContextId());
        assertEquals(securedInfo.getPlatformDeploymentId(), retVal.getPlatformDeploymentId());
        assertEquals(0L, retVal.getCreatedBy());
    }

    @Test
    public void testFillContextInfoLeavesCreatedByWhenUserNotFound() {
        when(ltiUserRepository.findFirstByUserKeyAndPlatformDeployment_KeyId(anyString(), anyLong())).thenReturn(null);
        ExperimentDto experimentDto = ExperimentDto.builder().createdBy(99L).build();

        ExperimentDto retVal = experimentService.fillContextInfo(experimentDto, securedInfo);

        assertEquals(99L, retVal.getCreatedBy());
    }

    @Test
    public void testDeleteConsentDocument() {
        experimentService.deleteConsentDocument(consentDocument);

        verify(consentDocumentRepository).delete(consentDocument);
    }

    @Test
    public void testGetEmptyExperimentReturnsNullWhenTitleNotBlank() {
        ExperimentDto experimentDto = ExperimentDto.builder().title("Not Blank").build();

        ExperimentDto retVal = experimentService.getEmptyExperiment(securedInfo, experimentDto);

        assertNull(retVal);
    }

    @Test
    public void testGetEmptyExperimentReturnsExperimentWhenBlankTitleFound() {
        ExperimentDto experimentDto = ExperimentDto.builder().build();
        when(experimentRepository.findByPlatformDeployment_KeyIdAndLtiContextEntity_ContextIdAndCreatedBy_UserKey(anyLong(), anyLong(), anyString())).thenReturn(List.of(experiment));

        ExperimentDto retVal = experimentService.getEmptyExperiment(securedInfo, experimentDto);

        assertNotNull(retVal);
        assertEquals(1L, retVal.getExperimentId());
    }

    @Test
    public void testGetEmptyExperimentReturnsNullWhenNoneBlankFound() {
        Experiment titledExperiment = mock(Experiment.class);
        when(titledExperiment.getTitle()).thenReturn("Has Title");
        ExperimentDto experimentDto = ExperimentDto.builder().build();
        when(experimentRepository.findByPlatformDeployment_KeyIdAndLtiContextEntity_ContextIdAndCreatedBy_UserKey(anyLong(), anyLong(), anyString())).thenReturn(List.of(titledExperiment));

        ExperimentDto retVal = experimentService.getEmptyExperiment(securedInfo, experimentDto);

        assertNull(retVal);
    }

    @Test
    public void testCopyDto() {
        ExperimentDto existingEmpty = ExperimentDto.builder().build();
        ExperimentDto experimentDto = ExperimentDto.builder()
            .description("desc")
            .distributionType("EVEN")
            .participationType("AUTO")
            .exposureType("BETWEEN")
            .title("Title")
            .started(Timestamp.from(Instant.now()))
            .build();

        experimentService.copyDto(existingEmpty, experimentDto);

        assertEquals("desc", existingEmpty.getDescription());
        assertEquals("EVEN", existingEmpty.getDistributionType());
        assertEquals("AUTO", existingEmpty.getParticipationType());
        assertEquals("BETWEEN", existingEmpty.getExposureType());
        assertEquals("Title", existingEmpty.getTitle());
        assertEquals(experimentDto.getStarted(), existingEmpty.getStarted());
    }

    @Test
    public void testBuildHeaders() {
        HttpHeaders retVal = experimentService.buildHeaders(UriComponentsBuilder.newInstance(), 5L);

        assertNotNull(retVal.getLocation());
        assertTrue(retVal.getLocation().toString().contains("/api/experiment/5"));
    }

    @Test
    public void testValidateTitleBlankDoesNotThrow() {
        assertDoesNotThrow(() -> experimentService.validateTitle("", 1L));
    }

    @Test
    public void testValidateTitleTooLongThrows() {
        Exception exception = assertThrows(TitleValidationException.class, () -> experimentService.validateTitle("a".repeat(256), 1L));

        assertEquals("Error 101: Experiment title must be 255 characters or less.", exception.getMessage());
    }

    @Test
    public void testValidateTitleAlreadyExistsThrows() {
        when(experimentRepository.existsByTitleAndLtiContextEntity_ContextIdAndExperimentIdIsNot(anyString(), anyLong(), eq(0L))).thenReturn(true);

        Exception exception = assertThrows(TitleValidationException.class, () -> experimentService.validateTitle("Duplicate", 1L));

        assertTrue(exception.getMessage().contains("Duplicate"));
    }

}
