package edu.iu.terracotta.service.app;

import edu.iu.terracotta.dao.entity.Experiment;
import edu.iu.terracotta.dao.entity.Group;
import edu.iu.terracotta.dao.exceptions.AssignmentNotMatchingException;
import edu.iu.terracotta.dao.exceptions.GroupNotMatchingException;

public interface GroupParticipantService {

    Group getUniqueGroupByConditionId(Long experimentId, String lmsAssignmentId, Long conditionId) throws GroupNotMatchingException, AssignmentNotMatchingException;
    Group nextGroup(Experiment experiment);

}
