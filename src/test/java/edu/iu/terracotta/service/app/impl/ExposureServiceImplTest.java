package edu.iu.terracotta.service.app.impl;

import edu.iu.terracotta.exceptions.ExperimentStartedException;
import edu.iu.terracotta.model.app.Experiment;
import edu.iu.terracotta.service.app.ExperimentService;
import edu.iu.terracotta.service.app.ExposureService;
import edu.iu.terracotta.utils.SpringBeanMockUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;


import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Disabled;

@SpringBootTest
@ActiveProfiles("test")
@ExtendWith(SpringExtension.class)
@Disabled("Test is broken and needs to be updated")
class ExposureServiceImplTest {

    /*
    This test is meant to be an example of mocking an external service so that only the service under testing is relied upon.
    The method being tested is marked as @Transactional, meaning none of the usual Spring Boot or Mockito mock annotations will work to
    mock the ExperimentService class. The workaround is a utility that is located in test/java/utils called SpringBeanMockUtil.
    The solution was found here: https://stackoverflow.com/questions/12857981/transactional-annotation-avoids-services-being-mocked.
    For methods not marked with @Transactional, Mockito's @Mock and @InjectMocks should be sufficient.
     */

    @Autowired
    ExposureService exposureService;

    @Test
    @DisplayName("createExposures() invalid path. Should throw an exception because the experiment is always started (mocked)")
    void test_invalid_createExposures() {
        ExperimentService experimentService = SpringBeanMockUtil.mockFieldOnBean(exposureService, ExperimentService.class);
        Mockito.when(experimentService.experimentStarted(Mockito.any(Experiment.class))).thenReturn(true);
        assertThrows(ExperimentStartedException.class, () -> exposureService.createExposures(1L));
        Mockito.verify(experimentService).experimentStarted(Mockito.any(Experiment.class));
    }
}

