package edu.iu.terracotta.service.app;

import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.exceptions.IdInPostException;
import edu.iu.terracotta.exceptions.TitleValidationException;
import edu.iu.terracotta.model.app.Condition;
import edu.iu.terracotta.model.app.dto.ConditionDto;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface ConditionService {
    List<ConditionDto> findAllByExperimentId(long experimentId);

    ConditionDto postCondition(ConditionDto conditionDto, long experimentId) throws IdInPostException, DataServiceException, TitleValidationException;

    ConditionDto toDto(Condition condition);

    Condition fromDto(ConditionDto conditionDto) throws DataServiceException;

    Condition save(Condition condition);

    Optional<Condition> findById(Long id);

    Condition findByConditionId(Long conditionId);

    ConditionDto getCondition(Long id);

    void saveAndFlush(Condition conditionToChange);

    void updateCondition(Map<Condition, ConditionDto> map);

    void deleteById(Long id) throws EmptyResultDataAccessException;

    boolean conditionBelongsToExperiment(Long experimentId, Long conditionId);

    boolean nameAlreadyExists(String name, Long experimentId, Long conditionId);

    boolean duplicateNameInPut(Map<Condition, ConditionDto> map, Condition condition);

    boolean isDefaultCondition(Long conditionId);

    HttpHeaders buildHeader(UriComponentsBuilder ucBuilder, Long experimentId, Long conditionId);

    void validateConditionName(String conditionName, String dtoName, Long experimentId, Long conditionId, boolean required) throws TitleValidationException;
}
