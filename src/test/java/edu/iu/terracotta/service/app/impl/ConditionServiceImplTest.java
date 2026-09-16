package edu.iu.terracotta.service.app.impl;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpHeaders;
import org.springframework.web.util.UriComponentsBuilder;

import edu.iu.terracotta.base.BaseTest;
import edu.iu.terracotta.dao.entity.Condition;
import edu.iu.terracotta.dao.exceptions.ConditionNotMatchingException;
import edu.iu.terracotta.dao.model.dto.ConditionDto;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.exceptions.ExperimentConditionLimitReachedException;
import edu.iu.terracotta.exceptions.IdInPostException;
import edu.iu.terracotta.exceptions.TitleValidationException;
import edu.iu.terracotta.utils.TextConstants;

public class ConditionServiceImplTest extends BaseTest {

    @InjectMocks private ConditionServiceImpl conditionService;

    @BeforeEach
    public void beforeEach() {
        MockitoAnnotations.openMocks(this);

        setup();
    }

    @Test
    public void testFindAllByExperimentId() {
        List<ConditionDto> retVal = conditionService.findAllByExperimentId(1L);

        assertEquals(1, retVal.size());
        assertEquals(CONDITION_TITLE, retVal.get(0).getName());
    }

    @Test
    public void testFindAllByExperimentIdEmptyWhenNull() {
        when(conditionRepository.findByExperiment_ExperimentIdOrderByConditionIdAsc(anyLong())).thenReturn(null);

        List<ConditionDto> retVal = conditionService.findAllByExperimentId(1L);

        assertTrue(retVal.isEmpty());
    }

    @Test
    public void testPostConditionSuccess() throws Exception {
        ConditionDto conditionDto = ConditionDto.builder().name("New Condition").build();
        when(conditionRepository.save(any(Condition.class))).thenReturn(condition);
        when(experimentRepository.findById(anyLong())).thenReturn(Optional.of(experiment));
        when(experimentRepository.findByUuid(experiment.getUuid())).thenReturn(experiment);

        ConditionDto retVal = conditionService.postCondition(conditionDto, 1L);

        assertNotNull(retVal);
        assertEquals(condition.getUuid(), retVal.getConditionId());
    }

    @Test
    public void testPostConditionIdInPostExceptionThrows() {
        ConditionDto conditionDto = ConditionDto.builder().conditionId(UUID.randomUUID()).build();

        Exception exception = assertThrows(IdInPostException.class, () -> conditionService.postCondition(conditionDto, 1L));

        assertEquals(TextConstants.ID_IN_POST_ERROR, exception.getMessage());
    }

    @Test
    public void testPostConditionDuplicateNameThrows() {
        when(conditionRepository.existsByNameAndExperiment_ExperimentIdAndConditionIdIsNot(anyString(), anyLong(), anyLong())).thenReturn(true);
        ConditionDto conditionDto = ConditionDto.builder().name("Dup Name").build();

        assertThrows(TitleValidationException.class, () -> conditionService.postCondition(conditionDto, 1L));
    }

    @Test
    public void testPostConditionNameTooLongThrows() {
        ConditionDto conditionDto = ConditionDto.builder().name("a".repeat(256)).build();

        Exception exception = assertThrows(TitleValidationException.class, () -> conditionService.postCondition(conditionDto, 1L));

        assertEquals("Error 101: Condition name must be 255 characters or less.", exception.getMessage());
    }

    @Test
    public void testPostConditionLimitReachedThrows() {
        when(conditionRepository.findByExperiment_ExperimentIdOrderByConditionIdAsc(anyLong())).thenReturn(Collections.nCopies(16, condition));
        ConditionDto conditionDto = ConditionDto.builder().name("New Condition").build();

        Exception exception = assertThrows(ExperimentConditionLimitReachedException.class, () -> conditionService.postCondition(conditionDto, 1L));

        assertEquals("Error 148: The experiment conditions limit of 16 conditions has been reached.", exception.getMessage());
    }

    @Test
    public void testPostConditionExperimentNotFoundWrapsDataServiceException() {
        when(experimentRepository.findById(anyLong())).thenReturn(Optional.empty());
        ConditionDto conditionDto = ConditionDto.builder().name("New Condition").build();

        Exception exception = assertThrows(DataServiceException.class, () -> conditionService.postCondition(conditionDto, 1L));

        assertEquals("Error 105: Unable to create condition: The experiment for the condition does not exist", exception.getMessage());
    }

    @Test
    public void testToDto() {
        when(condition.getDefaultCondition()).thenReturn(true);
        when(condition.getDistributionPct()).thenReturn(50F);

        ConditionDto retVal = conditionService.toDto(condition);

        assertEquals(condition.getUuid(), retVal.getConditionId());
        assertEquals(experiment.getUuid(), retVal.getExperimentId());
        assertEquals(CONDITION_TITLE, retVal.getName());
        assertTrue(retVal.getDefaultCondition());
        assertEquals(50F, retVal.getDistributionPct());
    }

    @Test
    public void testFromDtoSuccess() throws DataServiceException {
        // conditionDto.getConditionId() (now a uuid) is intentionally not set on a new Condition
        // by fromDto - see the comment on ConditionServiceImpl.fromDto - so it isn't asserted here,
        // mirroring ExperimentServiceImplTest#testFromDtoSuccess.
        ConditionDto conditionDto = ConditionDto.builder().conditionId(UUID.randomUUID()).experimentId(experiment.getUuid()).name("Condition A").defaultCondition(true).distributionPct(50F).build();
        when(experimentRepository.findByUuid(experiment.getUuid())).thenReturn(experiment);

        Condition retVal = conditionService.fromDto(conditionDto);

        assertEquals("Condition A", retVal.getName());
        assertEquals(experiment, retVal.getExperiment());
        assertTrue(retVal.getDefaultCondition());
        assertEquals(50F, retVal.getDistributionPct());
    }

    @Test
    public void testFromDtoExperimentNotFoundThrows() {
        when(experimentRepository.findById(anyLong())).thenReturn(Optional.empty());
        ConditionDto conditionDto = ConditionDto.builder().build();

        Exception exception = assertThrows(DataServiceException.class, () -> conditionService.fromDto(conditionDto));

        assertEquals("The experiment for the condition does not exist", exception.getMessage());
    }

    @Test
    public void testFindByConditionId() {
        when(conditionRepository.findByConditionId(anyLong())).thenReturn(condition);

        Condition retVal = conditionService.findByConditionId(1L);

        assertEquals(condition, retVal);
    }

    @Test
    public void testGetConditionByUuidFound() throws Exception {
        UUID uuid = condition.getUuid();
        when(conditionRepository.findByUuid(uuid)).thenReturn(condition);

        Condition retVal = conditionService.getConditionByUuid(uuid);

        assertEquals(condition, retVal);
    }

    @Test
    public void testGetConditionByUuidNotFoundThrows() {
        UUID uuid = UUID.randomUUID();
        when(conditionRepository.findByUuid(uuid)).thenReturn(null);

        Exception exception = assertThrows(ConditionNotMatchingException.class, () -> conditionService.getConditionByUuid(uuid));

        assertTrue(exception.getMessage().startsWith("Error 108"));
    }

    @Test
    public void testGetCondition() {
        when(conditionRepository.findByConditionId(anyLong())).thenReturn(condition);

        ConditionDto retVal = conditionService.getCondition(1L);

        assertEquals(condition.getUuid(), retVal.getConditionId());
    }

    @Test
    public void testUpdateCondition() {
        ConditionDto conditionDto = ConditionDto.builder().name("Updated Name").defaultCondition(true).distributionPct(25F).build();
        Map<Condition, ConditionDto> map = new LinkedHashMap<>();
        map.put(condition, conditionDto);

        conditionService.updateCondition(map);

        verify(condition).setName("Updated Name");
        verify(condition).setDefaultCondition(true);
        verify(condition).setDistributionPct(25F);
        verify(conditionRepository).save(condition);
    }

    @Test
    public void testDeleteById() {
        conditionService.deleteById(1L);

        verify(conditionRepository).deleteByConditionId(1L);
    }

    @Test
    public void testDuplicateNameInPutTrue() {
        when(condition.getName()).thenReturn("Same Name");
        Condition conditionInPut = mock(Condition.class);
        when(conditionInPut.getName()).thenReturn("Same Name");
        Map<Condition, ConditionDto> map = new LinkedHashMap<>();
        map.put(conditionInPut, ConditionDto.builder().build());

        assertTrue(conditionService.duplicateNameInPut(map, condition));
    }

    @Test
    public void testDuplicateNameInPutFalse() {
        when(condition.getName()).thenReturn("Name A");
        Condition conditionInPut = mock(Condition.class);
        when(conditionInPut.getName()).thenReturn("Name B");
        Map<Condition, ConditionDto> map = new LinkedHashMap<>();
        map.put(conditionInPut, ConditionDto.builder().build());

        assertFalse(conditionService.duplicateNameInPut(map, condition));
    }

    @Test
    public void testIsDefaultConditionTrue() {
        when(conditionRepository.existsByConditionIdAndDefaultCondition(anyLong(), eq(true))).thenReturn(true);

        assertTrue(conditionService.isDefaultCondition(1L));
    }

    @Test
    public void testIsDefaultConditionFalse() {
        when(conditionRepository.existsByConditionIdAndDefaultCondition(anyLong(), eq(true))).thenReturn(false);

        assertFalse(conditionService.isDefaultCondition(1L));
    }

    @Test
    public void testBuildHeader() {
        UUID experimentUuid = UUID.randomUUID();
        UUID conditionUuid = UUID.randomUUID();

        HttpHeaders retVal = conditionService.buildHeader(UriComponentsBuilder.newInstance(), experimentUuid, conditionUuid);

        assertNotNull(retVal);
        assertTrue(retVal.getLocation().toString().contains("/api/experiments/" + experimentUuid + "/conditions/" + conditionUuid));
    }

    @Test
    public void testValidateConditionNameValidDoesNotThrow() {
        assertDoesNotThrow(() -> conditionService.validateConditionName("", "Valid Name", 1L, 0L, true));
    }

    @Test
    public void testValidateConditionNameRequiredBlankThrows() {
        Exception exception = assertThrows(TitleValidationException.class, () -> conditionService.validateConditionName("", "", 1L, 0L, true));

        assertEquals("Error 100: Please give the condition a name.", exception.getMessage());
    }

    @Test
    public void testValidateConditionNameTooLongThrows() {
        Exception exception = assertThrows(TitleValidationException.class, () -> conditionService.validateConditionName("", "a".repeat(256), 1L, 0L, false));

        assertEquals("Error 101: Condition name must be 255 characters or less.", exception.getMessage());
    }

    @Test
    public void testValidateConditionNameDuplicateThrows() {
        when(conditionRepository.existsByNameAndExperiment_ExperimentIdAndConditionIdIsNot(anyString(), anyLong(), anyLong())).thenReturn(true);

        Exception exception = assertThrows(TitleValidationException.class, () -> conditionService.validateConditionName("", "Dup Name", 1L, 0L, false));

        assertTrue(exception.getMessage().startsWith("Error 102:"));
    }

    @Test
    public void testValidateConditionNamesRequiredBlankThrows() {
        // required=true throws for the blank name before the per-item uuid resolution loop runs,
        // so the conditionId here doesn't need a matching findByUuid stub.
        List<ConditionDto> conditionDtoList = List.of(ConditionDto.builder().conditionId(UUID.randomUUID()).name("").build());

        Exception exception = assertThrows(TitleValidationException.class, () -> conditionService.validateConditionNames(conditionDtoList, 1L, true));

        assertEquals("Error 100: Please give the condition a name.", exception.getMessage());
    }

    @Test
    public void testValidateConditionNamesTooLongThrows() {
        List<ConditionDto> conditionDtoList = List.of(ConditionDto.builder().conditionId(UUID.randomUUID()).name("a".repeat(256)).build());

        Exception exception = assertThrows(TitleValidationException.class, () -> conditionService.validateConditionNames(conditionDtoList, 1L, true));

        assertEquals("Error 101: Condition name must be 255 characters or less.", exception.getMessage());
    }

    @Test
    public void testValidateConditionNamesNoDuplicatesDoesNotThrow() {
        UUID conditionUuid = UUID.randomUUID();
        when(conditionRepository.findByUuid(conditionUuid)).thenReturn(condition);
        List<ConditionDto> conditionDtoList = List.of(ConditionDto.builder().conditionId(conditionUuid).name("Name A").build());
        when(conditionRepository.findByNameAndExperiment_ExperimentIdAndConditionIdIsNotOrderByConditionIdAsc(anyString(), anyLong(), anyLong())).thenReturn(Collections.emptyList());

        assertDoesNotThrow(() -> conditionService.validateConditionNames(conditionDtoList, 1L, false));
    }

    @Test
    public void testValidateConditionNamesDuplicateAcrossEntriesThrows() {
        UUID conditionUuid1 = UUID.randomUUID();
        UUID conditionUuid2 = UUID.randomUUID();
        Condition condition1 = mock(Condition.class);
        when(condition1.getConditionId()).thenReturn(1L);
        Condition condition2 = mock(Condition.class);
        when(condition2.getConditionId()).thenReturn(2L);
        when(conditionRepository.findByUuid(conditionUuid1)).thenReturn(condition1);
        when(conditionRepository.findByUuid(conditionUuid2)).thenReturn(condition2);

        ConditionDto dto1 = ConditionDto.builder().conditionId(conditionUuid1).name("Same").build();
        ConditionDto dto2 = ConditionDto.builder().conditionId(conditionUuid2).name("Same").build();
        List<ConditionDto> conditionDtoList = List.of(dto1, dto2);

        Condition conflicting = mock(Condition.class);
        when(conflicting.getConditionId()).thenReturn(2L);
        when(conflicting.getName()).thenReturn("Same");

        when(conditionRepository.findByNameAndExperiment_ExperimentIdAndConditionIdIsNotOrderByConditionIdAsc(eq("Same"), anyLong(), eq(1L))).thenReturn(List.of(conflicting));
        when(conditionRepository.findByNameAndExperiment_ExperimentIdAndConditionIdIsNotOrderByConditionIdAsc(eq("Same"), anyLong(), eq(2L))).thenReturn(Collections.emptyList());

        Exception exception = assertThrows(TitleValidationException.class, () -> conditionService.validateConditionNames(conditionDtoList, 1L, false));

        assertTrue(exception.getMessage().startsWith("Error 102:"));
    }

}
