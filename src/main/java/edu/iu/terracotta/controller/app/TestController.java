package edu.iu.terracotta.controller.app;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import java.security.Principal;

@Controller
@SuppressWarnings("rawtypes")
@RequestMapping(value = TestController.REQUEST_ROOT, produces = MediaType.APPLICATION_JSON_VALUE)
public class TestController {

    public static final String REQUEST_ROOT = "api/test";

    @GetMapping("/general")
    @PreAuthorize("hasAnyRole('GENERAL')")
    public ResponseEntity sampleSecureEndpointAny() {
        return new ResponseEntity<>("Welcome", HttpStatus.OK);
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity sampleSecureEndpointAdmin() {
        return new ResponseEntity<>("Welcome", HttpStatus.OK);
    }

    @GetMapping("/instructor")
    @PreAuthorize("hasRole('INSTRUCTOR')")
    public ResponseEntity sampleSecureEndpointInstructor(@AuthenticationPrincipal Principal principal) {
        return new ResponseEntity<>("Welcome", HttpStatus.OK);
    }

    @GetMapping("/student")
    @PreAuthorize("hasAnyRole('STUDENT','ADMIN')")
    public ResponseEntity sampleSecureEndpointStudent() {
        return new ResponseEntity<>("Welcome", HttpStatus.OK);
    }

}
