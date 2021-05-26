package edu.iu.terracotta.service.app;

import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.model.app.Group;
import edu.iu.terracotta.model.app.dto.GroupDto;
import org.springframework.dao.EmptyResultDataAccessException;

import java.util.List;
import java.util.Optional;

public interface GroupService {
    List<Group> findAllByExperimentId(long experimentId);

    Optional<Group> findOneByGroupId(long groupId);

    GroupDto toDto(Group group);

    Group fromDto(GroupDto groupDto) throws DataServiceException;

    Group save(Group group);

    Optional<Group> findById(Long id);

    void saveAndFlush(Group groupToChange);

    void deleteById(Long id) throws EmptyResultDataAccessException;

    boolean groupBelongsToExperiment(Long experimentId, Long groupId);
}
