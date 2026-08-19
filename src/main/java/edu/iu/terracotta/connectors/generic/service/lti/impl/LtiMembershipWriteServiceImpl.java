package edu.iu.terracotta.connectors.generic.service.lti.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import edu.iu.terracotta.connectors.generic.dao.entity.lti.LtiMembershipEntity;
import edu.iu.terracotta.connectors.generic.dao.repository.lti.LtiMembershipRepository;
import edu.iu.terracotta.connectors.generic.service.lti.LtiMembershipWriteService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LtiMembershipWriteServiceImpl implements LtiMembershipWriteService {

    private final LtiMembershipRepository ltiMembershipRepository;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public LtiMembershipEntity insert(LtiMembershipEntity ltiMembershipEntity) {
        return ltiMembershipRepository.save(ltiMembershipEntity);
    }

}
