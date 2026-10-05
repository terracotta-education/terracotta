package edu.iu.terracotta.service.app;

import edu.iu.terracotta.dao.entity.Condition;
import edu.iu.terracotta.dao.exceptions.ConditionNotMatchingException;
import edu.iu.terracotta.dao.model.dto.ConditionDto;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.exceptions.ExperimentConditionLimitReachedException;
import edu.iu.terracotta.exceptions.IdInPostException;
import edu.iu.terracotta.exceptions.TitleValidationException;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface ConditionService {

    List<ConditionDto> findAllByExperimentId(long experimentId);
    ConditionDto postCondition(ConditionDto conditionDto, long experimentId) throws IdInPostException, DataServiceException, TitleValidationException, ExperimentConditionLimitReachedException;
    ConditionDto toDto(Condition condition);
    Condition fromDto(ConditionDto conditionDto) throws DataServiceException;
    Condition findByConditionId(Long conditionId);
    Condition getConditionByUuid(UUID uuid) throws ConditionNotMatchingException;
    long getConditionIdByUuid(UUID uuid) throws ConditionNotMatchingException;
    ConditionDto getCondition(Long id);
    void updateCondition(Map<Condition, ConditionDto> map);
    void deleteById(Long id) throws EmptyResultDataAccessException;
    boolean duplicateNameInPut(Map<Condition, ConditionDto> map, Condition condition);
    boolean isDefaultCondition(Long conditionId);
    HttpHeaders buildHeader(UriComponentsBuilder ucBuilder, UUID experimentId, UUID conditionId);
    void validateConditionName(String conditionName, String dtoName, Long experimentId, Long conditionId, boolean required) throws TitleValidationException;
    void  validateConditionNames(List<ConditionDto> conditionDtoList, Long experimentId, boolean required)  throws TitleValidationException, ConditionNotMatchingException;

}
