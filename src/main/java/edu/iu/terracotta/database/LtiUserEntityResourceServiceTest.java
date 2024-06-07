package edu.iu.terracotta.database;


import edu.iu.terracotta.model.LtiUserEntity;
import edu.iu.terracotta.service.common.ResourceService;
import org.springframework.stereotype.Component;

@Component
@SuppressWarnings({"PMD.UncommentedEmptyMethodBody"})
public class LtiUserEntityResourceServiceTest implements ResourceService<LtiUserEntity> {

    private static final String USERS_RESOURCE = "classpath:test_data/users";

    @Override
    public String getDirectoryPath() {
        return USERS_RESOURCE;
    }

    @Override
    public void setDefaults() {
    }

}
