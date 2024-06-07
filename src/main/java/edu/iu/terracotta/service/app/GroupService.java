package edu.iu.terracotta.service.app;

import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.exceptions.IdInPostException;
import edu.iu.terracotta.exceptions.TitleValidationException;
import edu.iu.terracotta.model.app.Group;
import edu.iu.terracotta.model.app.dto.GroupDto;
import edu.iu.terracotta.model.oauth2.SecuredInfo;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

public interface GroupService {

    List<Group> findAllByExperimentId(long experimentId);
    List<GroupDto> getGroups(Long experimentId);
    GroupDto postGroup(GroupDto groupDto, long experimentId) throws IdInPostException, DataServiceException;
    Group getGroup(Long id);
    GroupDto toDto(Group group);
    Group fromDto(GroupDto groupDto) throws DataServiceException;
    void updateGroup(Long groupId, GroupDto groupDto) throws TitleValidationException;
    void deleteById(Long id) throws EmptyResultDataAccessException;
    void createAndAssignGroupsToConditionsAndExposures(Long experimentId, SecuredInfo securedInfo, boolean isCustom) throws DataServiceException;
    void validateTitle(String title) throws TitleValidationException;
    HttpHeaders buildHeaders(UriComponentsBuilder ucBuilder, Long experimentId, Long groupId);

}
