package edu.iu.terracotta.service.app;

import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.model.app.Condition;
import edu.iu.terracotta.model.app.dto.ConditionDto;
import org.springframework.dao.EmptyResultDataAccessException;

import java.util.List;
import java.util.Optional;

public interface ConditionService {
    List<Condition> findAllByExperimentId(long experimentId);

    Optional<Condition> findOneByConditionId(long conditionId);

    ConditionDto toDto(Condition condition);

    Condition fromDto(ConditionDto conditionDto) throws DataServiceException;

    Condition save(Condition condition);

    //needed??
    Optional<Condition> findById(Long id);

    void saveAndFlush(Condition conditionToChange);

    void deleteById(Long id) throws EmptyResultDataAccessException;

    boolean conditionBelongsToExperiment(Long experimentId, Long conditionId);
}