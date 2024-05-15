package edu.iu.terracotta.database;

import edu.iu.terracotta.model.test.Conditions;
import edu.iu.terracotta.service.common.ResourceService;
import org.springframework.stereotype.Component;


@Component
@SuppressWarnings({"PMD.UncommentedEmptyMethodBody"})
public class ConditionResourceService implements ResourceService<Conditions> {

    private static final String CONDITIONS_RESOURCE = "classpath:test_data/conditions";

    @Override
    public String getDirectoryPath() {
        return CONDITIONS_RESOURCE;
    }

    @Override
    public void setDefaults() {

    }

}
