package edu.iu.terracotta.service.app;

import edu.iu.terracotta.exceptions.AssignmentNotMatchingException;
import edu.iu.terracotta.exceptions.GroupNotMatchingException;
import edu.iu.terracotta.model.app.Experiment;
import edu.iu.terracotta.model.app.Group;

public interface GroupParticipantService {

    Group getUniqueGroupByConditionId(Long experimentId, String canvasAssignmentId, Long conditionId) throws GroupNotMatchingException, AssignmentNotMatchingException;

    Group nextGroup(Experiment experiment);

}
