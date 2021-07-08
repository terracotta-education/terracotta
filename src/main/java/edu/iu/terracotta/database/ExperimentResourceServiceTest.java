package edu.iu.terracotta.database;

import edu.iu.terracotta.model.app.Experiment;
import edu.iu.terracotta.service.common.ResourceService;
import org.springframework.stereotype.Component;

@Component
public class ExperimentResourceServiceTest implements ResourceService<Experiment> {

    static final String EXPERIMENTS_RESOURCE = "classpath:test_data/experiments";

    @Override
    public String getDirectoryPath() { return EXPERIMENTS_RESOURCE; }

    @Override
    public void setDefaults(){}
}
