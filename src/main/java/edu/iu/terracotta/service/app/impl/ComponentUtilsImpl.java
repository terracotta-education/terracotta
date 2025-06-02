package edu.iu.terracotta.service.app.impl;

import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import edu.iu.terracotta.connectors.generic.dao.entity.lti.LtiUserEntity;
import edu.iu.terracotta.dao.entity.Assignment;
import edu.iu.terracotta.dao.entity.messaging.container.MessageContainer;
import edu.iu.terracotta.dao.repository.AssignmentRepository;
import edu.iu.terracotta.dao.repository.messaging.container.MessageContainerRepository;
import edu.iu.terracotta.service.app.ComponentUtils;

@Service
public class ComponentUtilsImpl implements ComponentUtils {

    @Autowired private AssignmentRepository assignmentRepository;
    @Autowired private MessageContainerRepository messageContainerRepository;

    @Override
    public int calculateNextOrder(long exposureId, LtiUserEntity owner) {
        // get a list of all assignments in the given exposure set; ordered by assignment order descending
        List<Assignment> exposureAssignments = assignmentRepository.findByExposure_ExposureIdAndSoftDeletedOrderByAssignmentOrderDesc(exposureId, false);
        // get a list of all message containers in the given exposure set; ordered by assignment order descending
        List<MessageContainer> exposureMessageContainers = messageContainerRepository.findAllByExposure_ExposureIdAndOwner_LmsUserIdOrderByConfiguration_ContainerOrderDesc(exposureId, owner.getLmsUserId());

        int nextAvailable = 1;

        if (CollectionUtils.isNotEmpty(exposureAssignments)) {
            // set the assignment order to be the next one in the list
            nextAvailable = exposureAssignments.get(0).getAssignmentOrder() + 1;
        }

        if (CollectionUtils.isNotEmpty(exposureMessageContainers) && exposureMessageContainers.get(0).getOrder() > nextAvailable) {
            // if the first message container order is greater than the next available, set the next available to be the first message container order
            nextAvailable = exposureMessageContainers.get(0).getOrder() + 1;
        }

        return nextAvailable;
    }

}
