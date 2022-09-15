package edu.iu.terracotta.service.app;

import edu.iu.terracotta.exceptions.AssignmentNotMatchingException;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.exceptions.GroupNotMatchingException;
import edu.iu.terracotta.exceptions.IdInPostException;
import edu.iu.terracotta.exceptions.TitleValidationException;
import edu.iu.terracotta.model.app.Experiment;
import edu.iu.terracotta.model.app.ExposureGroupCondition;
import edu.iu.terracotta.model.app.Group;
import edu.iu.terracotta.model.app.dto.GroupDto;
import edu.iu.terracotta.model.oauth2.SecuredInfo;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Optional;

public interface GroupService {
    List<Group> findAllByExperimentId(long experimentId);

    List<GroupDto> getGroups(Long experimentId);

    GroupDto postGroup(GroupDto groupDto, long experimentId) throws IdInPostException, DataServiceException;

    Group getGroup(Long id);

    GroupDto toDto(Group group);

    Group fromDto(GroupDto groupDto) throws DataServiceException;

    Group save(Group group);

    ExposureGroupCondition saveExposureGroupCondition(ExposureGroupCondition exposureGroupCondition);

    Optional<Group> findById(Long id);

    void updateGroup(Long groupId, GroupDto groupDto) throws TitleValidationException;

    void saveAndFlush(Group groupToChange);

    void deleteById(Long id) throws EmptyResultDataAccessException;

    boolean groupBelongsToExperiment(Long experimentId, Long groupId);

    void createAndAssignGroupsToConditionsAndExposures(Long experimentId, SecuredInfo securedInfo, boolean isCustom) throws DataServiceException;

    boolean existsByExperiment_ExperimentIdAndGroupId(Long experimentId, Long groupId);

    Group nextGroup(Experiment experiment);

    void validateTitle(String title) throws TitleValidationException;

    HttpHeaders buildHeaders(UriComponentsBuilder ucBuilder, Long experimentId, Long groupId);

    Group getUniqueGroupByConditionId(Long experimentId, String canvasAssignmentId, Long conditionId) throws GroupNotMatchingException, AssignmentNotMatchingException;

}
