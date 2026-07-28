package edu.iu.terracotta.controller.app;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.security.Principal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import edu.iu.terracotta.base.BaseTest;

@SuppressWarnings("unchecked")
public class TestControllerTest extends BaseTest {

    @InjectMocks private TestController testController;

    @BeforeEach
    public void beforeEach() {
        MockitoAnnotations.openMocks(this);

        setup();
    }

    @Test
    void sampleSecureEndpointAnyTest() {
        ResponseEntity<String> ret = testController.sampleSecureEndpointAny();

        assertEquals(HttpStatus.OK, ret.getStatusCode());
        assertEquals("Welcome", ret.getBody());
    }

    @Test
    void sampleSecureEndpointAdminTest() {
        ResponseEntity<String> ret = testController.sampleSecureEndpointAdmin();

        assertEquals(HttpStatus.OK, ret.getStatusCode());
        assertEquals("Welcome", ret.getBody());
    }

    @Test
    void sampleSecureEndpointInstructorTest() {
        Principal principal = () -> "instructor";

        ResponseEntity<String> ret = testController.sampleSecureEndpointInstructor(principal);

        assertEquals(HttpStatus.OK, ret.getStatusCode());
        assertEquals("Welcome", ret.getBody());
    }

    @Test
    void sampleSecureEndpointInstructorNullPrincipalTest() {
        ResponseEntity<String> ret = testController.sampleSecureEndpointInstructor(null);

        assertEquals(HttpStatus.OK, ret.getStatusCode());
        assertEquals("Welcome", ret.getBody());
    }

    @Test
    void sampleSecureEndpointStudentTest() {
        ResponseEntity<String> ret = testController.sampleSecureEndpointStudent();

        assertEquals(HttpStatus.OK, ret.getStatusCode());
        assertEquals("Welcome", ret.getBody());
    }

}
