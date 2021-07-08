package edu.iu.terracotta.database;

import edu.iu.terracotta.model.LtiContextEntity;
import edu.iu.terracotta.service.common.ResourceService;
import org.springframework.stereotype.Component;

@Component
public class LtiContextEntityResourceServiceTest implements ResourceService<LtiContextEntity> {

    final static String LTI_CONTEXT_ENTITY_RESOURCE = "classpath:test_data/lti_context_entities";

    @Override
    public String getDirectoryPath(){ return LTI_CONTEXT_ENTITY_RESOURCE; }

    @Override
    public void setDefaults() { }
}
