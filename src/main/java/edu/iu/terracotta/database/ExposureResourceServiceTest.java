package edu.iu.terracotta.database;

import edu.iu.terracotta.model.app.Exposure;
import edu.iu.terracotta.service.common.ResourceService;
import org.springframework.stereotype.Component;

@Component
@SuppressWarnings({"PMD.UncommentedEmptyMethodBody"})
public class ExposureResourceServiceTest implements ResourceService<Exposure> {

    private static final String EXPOSURE_RESOURCE = "classpath:test_data/exposures";

    @Override
    public String getDirectoryPath() {
        return EXPOSURE_RESOURCE;
    }

    @Override
    public void setDefaults() {

    }
}
