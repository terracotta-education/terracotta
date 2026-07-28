package edu.iu.terracotta.controller.app;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

import edu.iu.terracotta.base.BaseTest;

public class ObsoleteControllerTest extends BaseTest {

    @InjectMocks private ObsoleteController obsoleteController;

    @BeforeEach
    public void beforeEach() {
        MockitoAnnotations.openMocks(this);

        setup();
    }

    @Test
    void assignmentTest() {
        String ret = obsoleteController.assignment();

        assertEquals("redirect:/app/app.html?obsolete=true&type=assignment", ret);
    }

}
