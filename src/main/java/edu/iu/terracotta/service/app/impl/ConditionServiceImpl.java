package edu.iu.terracotta.service.app.impl;

import edu.iu.terracotta.dao.entity.Condition;
import edu.iu.terracotta.dao.entity.Experiment;
import edu.iu.terracotta.dao.exceptions.ConditionNotMatchingException;
import edu.iu.terracotta.dao.model.dto.ConditionDto;
import edu.iu.terracotta.dao.repository.ConditionRepository;
import edu.iu.terracotta.dao.repository.ExperimentRepository;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.exceptions.ExperimentConditionLimitReachedException;
import edu.iu.terracotta.exceptions.IdInPostException;
import edu.iu.terracotta.exceptions.TitleValidationException;
import edu.iu.terracotta.service.app.ConditionService;
import edu.iu.terracotta.utils.TextConstants;
import lombok.RequiredArgsConstructor;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@SuppressWarnings({"PMD.PreserveStackTrace"})
public class ConditionServiceImpl implements ConditionService {

    private static final int CONDITION_COUNT_ALLOWED = 16;

    private final ConditionRepository conditionRepository;
    private final ExperimentRepository experimentRepository;

    @Override
    public List<ConditionDto> findAllByExperimentId(long experimentId) {
        return CollectionUtils.emptyIfNull(conditionRepository.findByExperiment_ExperimentIdOrderByConditionIdAsc(experimentId)).stream()
            .map(this::toDto)
            .toList();
    }

    @Override
    public ConditionDto postCondition(ConditionDto conditionDto, long experimentId) throws IdInPostException, DataServiceException, TitleValidationException, ExperimentConditionLimitReachedException {
        if (conditionDto.getConditionId() != null) {
            throw new IdInPostException(TextConstants.ID_IN_POST_ERROR);
        }

        validateConditionName("", conditionDto.getName(), experimentId, 0L, false);
        validateMaximumConditionsNotReached(experimentId);

        conditionDto.setExperimentId(experimentRepository.findUuidByExperimentId(experimentId).orElse(null));
        Condition condition;

        try {
            condition = fromDto(conditionDto);
        } catch (DataServiceException e) {
            throw new DataServiceException(String.format("Error 105: Unable to create condition: %s", e.getMessage()), e);
        }

        return toDto(save(condition));
    }

    @Override
    public ConditionDto toDto(Condition condition) {
        ConditionDto conditionDto = new ConditionDto();
        conditionDto.setConditionId(condition.getUuid());
        conditionDto.setExperimentId(condition.getExperiment().getUuid());
        conditionDto.setName(condition.getName());
        conditionDto.setDefaultCondition(condition.getDefaultCondition());
        conditionDto.setDistributionPct(condition.getDistributionPct());

        return conditionDto;
    }

    @Override
    public Condition fromDto(ConditionDto conditionDto) throws DataServiceException {
        // conditionDto.getConditionId() (now a uuid) is intentionally not set on a new Condition
        // here - the controller already rejects a create request that carries one
        // (IdInPostException), and the real numeric id/uuid are both IDENTITY/@PrePersist
        // generated at insert time regardless.
        Condition condition = new Condition();
        Experiment experiment = conditionDto.getExperimentId() != null ? experimentRepository.findByUuid(conditionDto.getExperimentId()) : null;

        if (experiment == null) {
            throw new DataServiceException("The experiment for the condition does not exist");
        }

        condition.setExperiment(experiment);
        condition.setName(conditionDto.getName());
        condition.setDefaultCondition(conditionDto.getDefaultCondition());
        condition.setDistributionPct(conditionDto.getDistributionPct());

        return condition;
    }

    private Condition save(Condition condition) {
        return conditionRepository.save(condition);
    }

    @Override
    public Condition findByConditionId(Long conditionId) {
        return conditionRepository.findByConditionId(conditionId);
    }

    @Override
    public Condition getConditionByUuid(UUID uuid) throws ConditionNotMatchingException {
        return Optional.ofNullable(conditionRepository.findByUuid(uuid))
            .orElseThrow(() -> new ConditionNotMatchingException(TextConstants.CONDITION_NOT_MATCHING));
    }

    @Override
    public long getConditionIdByUuid(UUID uuid) throws ConditionNotMatchingException {
        return conditionRepository.findIdByUuid(uuid)
            .orElseThrow(() -> new ConditionNotMatchingException(TextConstants.CONDITION_NOT_MATCHING));
    }

    @Override
    public ConditionDto getCondition(Long id) {
        return toDto(findByConditionId(id));
    }

    @Override
    @Transactional
    public void updateCondition(Map<Condition, ConditionDto> map) {
        for (Map.Entry<Condition, ConditionDto> entry : map.entrySet()) {
            Condition condition = entry.getKey();
            ConditionDto conditionDto = entry.getValue();
            condition.setName(conditionDto.getName());
            condition.setDefaultCondition(conditionDto.getDefaultCondition());
            condition.setDistributionPct(conditionDto.getDistributionPct());
            save(condition);
        }
    }

    @Override
    public void deleteById(Long id) throws EmptyResultDataAccessException {
        conditionRepository.deleteByConditionId(id);
    }

    @Override
    public boolean duplicateNameInPut(Map<Condition, ConditionDto> map, Condition condition) {
        for (Map.Entry<Condition, ConditionDto> entry : map.entrySet()) {
            Condition conditionInPut = entry.getKey();

            if (condition.getName().equals(conditionInPut.getName())) {
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean isDefaultCondition(Long conditionId) {
        return conditionRepository.existsByConditionIdAndDefaultCondition(conditionId, true);
    }

    @Override
    public HttpHeaders buildHeader(UriComponentsBuilder ucBuilder, UUID experimentId, UUID conditionId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(ucBuilder.path("/api/experiments/{experimentId}/conditions/{conditionId}")
                .buildAndExpand(experimentId, conditionId).toUri());

        return headers;
    }

    @Override
    public void validateConditionName(String conditionName, String dtoName, Long experimentId, Long conditionId, boolean required) throws TitleValidationException {
        if (required) {
            if (StringUtils.isAllBlank(dtoName, conditionName)) {
                throw new TitleValidationException("Error 100: Please give the condition a name.");
            }
        }

        if (StringUtils.isNotBlank(dtoName)) {
            if (dtoName.length() > 255) {
                throw new TitleValidationException("Error 101: Condition name must be 255 characters or less.");
            }

            if (conditionRepository.existsByNameAndExperiment_ExperimentIdAndConditionIdIsNot(dtoName, experimentId, conditionId)) {
                throw new TitleValidationException("Error 102: Unable to create the condition. A condition with title \"" + dtoName + "\" already exists in this experiment. It is possible " +
                        "that one of the other conditions has that name and has not been updated with a new one yet. If that is the case and you wish to use this name, " +
                        "please change that condition's name first, then try again.");
            }
        }
    }

    @Override
    public void validateConditionNames(List<ConditionDto> conditionDtoList, Long experimentId, boolean required) throws TitleValidationException, ConditionNotMatchingException {
        if (required) {
            for (ConditionDto conditionDto : conditionDtoList) {
                if (StringUtils.isBlank(conditionDto.getName())) {
                    throw new TitleValidationException("Error 100: Please give the condition a name.");
                }

                if (StringUtils.isNotBlank(conditionDto.getName())) {
                    if (conditionDto.getName().length() > 255) {
                        throw new TitleValidationException("Error 101: Condition name must be 255 characters or less.");
                    }
                }
            }
        }

        // conditionDto.getConditionId() is now a uuid (the DTO's wire id), but the duplicate-name
        // lookup and comparison below need each condition's numeric id, so resolve them all up
        // front (identity-keyed, since ConditionDto has no field-based equals/hashCode).
        Map<ConditionDto, Long> conditionIdsByDto = new IdentityHashMap<>();

        for (ConditionDto conditionDto : conditionDtoList) {
            conditionIdsByDto.put(conditionDto, getConditionByUuid(conditionDto.getConditionId()).getConditionId());
        }

        for (ConditionDto condto : conditionDtoList) {
            List<Condition> conditions = conditionRepository.findByNameAndExperiment_ExperimentIdAndConditionIdIsNotOrderByConditionIdAsc(condto.getName(), experimentId, conditionIdsByDto.get(condto));

            if (CollectionUtils.isEmpty(conditions)) {
                continue;
            }

            for (Condition con : conditions) {
                List<ConditionDto> duplicates = conditionDtoList.stream()
                    .filter(co -> {
                        return conditionIdsByDto.get(co).equals(con.getConditionId()) && co.getName().equals(con.getName());
                    })
                    .toList();

                if (!duplicates.isEmpty()) {
                    throw new TitleValidationException("Error 102: Unable to create the condition. A condition with title \"" + condto.getName() + "\" already exists in this experiment. It is possible " +
                            "that one of the other conditions has that name and has not been updated with a new one yet. If that is the case and you wish to use this name, " +
                            "please change that condition's name first, then try again.");
                }
            }
        }
    }

    private void validateMaximumConditionsNotReached(long experimentId) throws ExperimentConditionLimitReachedException {
        if (conditionRepository.findByExperiment_ExperimentIdOrderByConditionIdAsc(experimentId).size() < CONDITION_COUNT_ALLOWED) {
            return;
        }

        throw new ExperimentConditionLimitReachedException("Error 148: The experiment conditions limit of 16 conditions has been reached.");
    }

}
