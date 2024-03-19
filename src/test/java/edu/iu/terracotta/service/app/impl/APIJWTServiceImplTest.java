package edu.iu.terracotta.service.app.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

import edu.iu.terracotta.BaseTest;
import edu.iu.terracotta.model.LtiContextEntity;
import edu.iu.terracotta.model.LtiUserEntity;

public class APIJWTServiceImplTest extends BaseTest {

    @InjectMocks private APIJWTServiceImpl apiJWTService;

    private Map<String, Object> customVars;

    @BeforeEach
    public void beforeEach() {
        MockitoAnnotations.openMocks(this);

        setup();

        when(lti3Request.getLtiTargetLinkUrl()).thenReturn("");
        when(lti3Request.getContext()).thenReturn(new LtiContextEntity());
        when(lti3Request.getKey()).thenReturn(platformDeployment);
        when(lti3Request.getUser()).thenReturn(new LtiUserEntity("userKey", null, platformDeployment));

        customVars = new HashMap<>();
        customVars.put("canvas_user_id", "123");
        customVars.put("canvas_user_global_id", "202570000000000141");
        customVars.put("canvas_login_id", "teststudent@iu.edu");
        customVars.put("canvas_user_name", "teststudent@iu.edu");
        customVars.put("canvas_course_id", "1154");
        customVars.put("canvas_assignment_id", "$Canvas.assignment.id");
        customVars.put("due_at", "$Canvas.assignment.dueAt.iso8601");
        customVars.put("lock_at", "$Canvas.assignment.lockAt.iso8601");
        customVars.put("unlock_at", "$Canvas.assignment.unlockAt.iso8601");

        when(lti3Request.getLtiCustom()).thenReturn(customVars);
        String privateKey = "-----BEGIN PRIVATE KEY-----MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQDovlSkSB1FnbHR4ROh2/kVxQtpRQQKK7iYIEpRh3veKF2j2eiSvAc0GbOx596L62KEXGNUrBeW1UR1d9UfP40qsm6yP8MM1knv8eGDyCpGfcUSEysWXBBs9yUbILdBBO3XBNP3zd21mQUGTDjOXQppjDm9JnKNGxVjmFQiMdsBx8nHDeLt+QygGRgHcQmpmiY7kXZl3r5CIoLY+hvtC67byhES/2fQ1XWoHvikRRBt2GHT93jZXJtgjA89ZOsSPqb6LTh1OZIqS12pYciS7TQ9zbeN2SwYfW+G9o8f8bAZxZz6iZVCUmwC6ZOaHZ8KKM7FauddhaXxW/70MdQqgTg/AgMBAAECggEAITyk+7zsqTdm4HEDC7dNL+Wuxn67n/Q0bU0XL+NoNgaPsMl6pBHD+ZW+CqbxKgwYSoyjBsF4sOqN1zSgs9CwiStoEX53jUrAzko9iUM5fk2Rqg4gthW5psX4f5JBeUCJ8o3W82lrwvYyOH8EEbxJs176E9/8tdfrSwjC4ws5mlw24LqXZ5GEJl1nwlAjGv1h3JPf2A457WXkpq0jZ4YejKbdJIzFLgF6B6HpDLHWjcJXeNAhjuiDxWqmsqqO41qOR+sUvTDPEVpdEB5sBWZxr0a3lJ1ZyXbYaZb6LSdtMJghk0CKNhVEpm2z4TZElQfaii3oG1oaQo4YlQ0zL4vmcQKBgQD6EvRcy+yMTBJi9G34CWbVFoAMrw7RsYkRmqPvdCY7eidn43fu0n/z+mw7bBtnmMZNaZs+HvaX3JqTTEXaj19W0lJg92OWK1sWur2T/GPw1GVVSm9niOP1RR4cxXYQqCQF3y9V89wRZ4X+K+F5i6rar3rgAu+U03adKJnPTHzF2QKBgQDuQj4Cehn3gpEAocWfLQeOQNbBfjBYWOmyHC2UQpXnuLCW3/zvcIssQEJTWGpX5556aXDs/SN/yJax+8E6MzwNC4HC4xU+3tBfJ3DqABR9U5Gb+EZ/lLKCXodLsxddB4TPVbivjC+LHN6/Vpj9Rsnl6m7+wgDkbkccnVhjfUsn1wKBgHZFDqLwowA7XhrExVmggKzYxli5VkXgNBZKT6wI/6fzfr2IfAlMLs2hqxxzZYaaX3bvMkev9yodYFG3qfXTBuEV+XX4qnW0LZFTYiOiI1Yb7Yzn9kY+HKm8NaCf1tXL37WTN1zsRzFIB7wM3sdQQc7JXVCistJtLFTphczfvMcJAoGBAMalgi/sf5PmT2E4f50sHP2Uv7kJreMrFoVCixnuvi85xDm2vJshuVeGqAX3VIq/+VjUaqucjqluo333ie4tY2b47hJ/5GnLue1r4++la2/maiOhR539ayvZBnKt+c+9ghSfwuDSP517z5e16s5Y4+KGqE5NkBLkgvOvmE8y2qN3AoGAcsKq+UWOYsi9LtmG6oM0w6S749z180j6zT/axzVowxfULNRXEzhqOSZTfe7zIAbhc09S68oliJdWkzlVRfPYyiL9oq5+gl/writb8if+VyeTdlJJsThfkGGrD8PiJoqMGIby8uzs7/EIPNWRiP/32DS58+6a5xAojVV0PHaM/mI=-----END PRIVATE KEY-----";
        when(ltiDataService.getOwnPrivateKey()).thenReturn(privateKey);
    }

    // Test the case where the LTI tool launch is not in an assignment context
    @Test
    public void testBuildJwtWithNoAllowedAttemptsCustomVariable() throws GeneralSecurityException, IOException {

        // Value is unreplaced when it is unavailable
        customVars.put("allowed_attempts", "$Canvas.assignment.allowedAttempts");
        customVars.put("student_attempts", "$Canvas.assignment.submission.studentAttempts");

        String jwt = apiJWTService.buildJwt(false, lti3Request);
        Map<String, Object> claims = this.apiJWTService.unsecureToken(jwt);

        assertFalse(claims.containsKey("allowedAttempts"));
        assertFalse(claims.containsKey("studentAttempts"));
    }

    // assignment context, test with allowed_attempts = null
    @Test
    public void testBuildJwtWithNullAllowedAttemptsCustomVariable() throws GeneralSecurityException, IOException {

        customVars.put("allowed_attempts", null);
        customVars.put("student_attempts", "3");

        String jwt = apiJWTService.buildJwt(false, lti3Request);
        Map<String, Object> claims = this.apiJWTService.unsecureToken(jwt);

        // populate allowedAttempts with -1 to indicate that there are unlimited
        // attempts
        assertEquals(-1, claims.get("allowedAttempts"));
        assertEquals(3, claims.get("studentAttempts"));
    }

    // assignment context, test with allowed_attempts = 3
    @Test
    public void testBuildJwtWithSomeAllowedAttemptsCustomVariable() throws GeneralSecurityException, IOException {

        customVars.put("allowed_attempts", "3");
        customVars.put("student_attempts", "1");

        String jwt = apiJWTService.buildJwt(false, lti3Request);
        Map<String, Object> claims = this.apiJWTService.unsecureToken(jwt);

        assertEquals(3, claims.get("allowedAttempts"));
        assertEquals(1, claims.get("studentAttempts"));
    }

    // assignment context, test with allowed_attempts = 3, student_attempts = null
    @Test
    public void testBuildJwtWithNoStudentAttemptsCustomVariable() throws GeneralSecurityException, IOException {

        customVars.put("allowed_attempts", "3");
        customVars.put("student_attempts", null);

        String jwt = apiJWTService.buildJwt(false, lti3Request);
        Map<String, Object> claims = this.apiJWTService.unsecureToken(jwt);

        assertEquals(3, claims.get("allowedAttempts"));
        assertEquals(0, claims.get("studentAttempts"));
    }

}
