package edu.iu.terracotta.controller.lti;

import edu.iu.terracotta.connectors.generic.dao.entity.lti.PlatformDeployment;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.ToolDeployment;
import edu.iu.terracotta.connectors.generic.dao.repository.lti.PlatformDeploymentRepository;
import edu.iu.terracotta.connectors.generic.dao.repository.lti.ToolDeploymentRepository;
import edu.iu.terracotta.utils.TextConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.util.UriComponentsBuilder;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * This controller is protected by basic authentication
 * Allows to read and change the configuration
 */
@Slf4j
@Controller
@Scope("session")
@RequiredArgsConstructor
@RequestMapping("/config")
public class LtiConfigurationController {

    private final PlatformDeploymentRepository platformDeploymentRepository;
    private final ToolDeploymentRepository toolDeploymentRepository;

    /**
     * To show the configurations.
     */
    @GetMapping("/")
    public ResponseEntity<List<PlatformDeployment>> displayConfigs(HttpServletRequest req) {
        List<PlatformDeployment> platformDeploymentListEntityList = platformDeploymentRepository.findAll();

        if (platformDeploymentListEntityList.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            // You many decide to return HttpStatus.NOT_FOUND
        }

        return new ResponseEntity<>(platformDeploymentListEntityList, HttpStatus.OK);
    }

    /**
     * To show the configurations.
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> displayConfig(@PathVariable long id, HttpServletRequest req) {
        Optional<PlatformDeployment> platformDeployment = platformDeploymentRepository.findById(id);

        if (platformDeployment.isPresent()) {
            return new ResponseEntity<>(platformDeployment.get(), HttpStatus.OK);
        }

        log.error("platformDeployment with id {} not found.", id);
        return new ResponseEntity<>("platformDeployment with id " + id + TextConstants.NOT_FOUND_SUFFIX, HttpStatus.NOT_FOUND);
    }

    @PostMapping("/")
    public ResponseEntity<String> createDeployment(@RequestBody PlatformDeployment platformDeployment, UriComponentsBuilder ucBuilder) {
        if (!platformDeploymentRepository.findByIssAndClientId(platformDeployment.getIss(), platformDeployment.getClientId()).isEmpty()) {
            log.error("Unable to create. A platformDeployment with iss [{}] and clientId [{}] already exists", platformDeployment.getIss(), platformDeployment.getClientId());
            return new ResponseEntity<String>("Unable to create. A platformDeployment with same key already exist.", HttpStatus.CONFLICT);
        }

        // always a new record: the request's own ID, UUID and relations are dropped, so a request
        // carrying another record's ID can't overwrite it. The rest is the platform's configuration,
        // which is what this admin-only endpoint is for.
        Set<ToolDeployment> requestedToolDeployments = platformDeployment.getToolDeployments();
        platformDeployment.setKeyId(0);
        platformDeployment.setUuid(null);
        platformDeployment.setToolDeployments(null);
        platformDeployment.setFeatures(null);

        // deepcode ignore MassAssignment: an admin-only (HTTP Basic) endpoint that sets a platform's whole configuration; its ID and relations are cleared above
        PlatformDeployment platformDeploymentSaved = platformDeploymentRepository.save(platformDeployment);

        if (requestedToolDeployments != null) {
            for (ToolDeployment toolDeployment : requestedToolDeployments) {
                // deepcode ignore MassAssignment: only the LTI deployment ID is taken from the request (see newToolDeployment)
                toolDeploymentRepository.save(newToolDeployment(toolDeployment, platformDeploymentSaved));
            }
        }

        log.info("Created platform deployment ID: [{}] for issuer: [{}] and client ID: [{}]", platformDeploymentSaved.getKeyId(), platformDeploymentSaved.getIss(), platformDeploymentSaved.getClientId());

        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(ucBuilder.path("/config/{id}").buildAndExpand(platformDeploymentSaved.getKeyId()).toUri());

        return new ResponseEntity<>(headers, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateDeployment(@PathVariable("id") long id, @RequestBody PlatformDeployment platformDeployment) {
        Optional<PlatformDeployment> platformDeploymentSearchResult = platformDeploymentRepository.findById(id);

        if (platformDeploymentSearchResult.isEmpty()) {
            log.error("Unable to update. PlatformDeployment with id {} not found.", id);
            return new ResponseEntity<>("Unable to update. User with id " + id + TextConstants.NOT_FOUND_SUFFIX, HttpStatus.NOT_FOUND);
        }

        PlatformDeployment platformDeploymentToChange = platformDeploymentSearchResult.get();
        platformDeploymentToChange.setOAuth2TokenUrl(platformDeployment.getOAuth2TokenUrl());
        platformDeploymentToChange.setClientId(platformDeployment.getClientId());
        platformDeploymentToChange.setIss(platformDeployment.getIss());
        platformDeploymentToChange.setOidcEndpoint(platformDeployment.getOidcEndpoint());
        platformDeploymentToChange.setJwksEndpoint(platformDeployment.getJwksEndpoint());
        platformDeploymentToChange.setEnableAutomaticDeployments(platformDeployment.getEnableAutomaticDeployments());

        // add any missing ToolDeployments
        if (platformDeployment.getToolDeployments() != null) {
            for (ToolDeployment toolDeployment : platformDeployment.getToolDeployments()) {
                if (platformDeploymentToChange.getToolDeployments().stream()
                        .noneMatch(td -> td.getLtiDeploymentId().equals(toolDeployment.getLtiDeploymentId()))) {
                    // deepcode ignore MassAssignment: only the LTI deployment ID is taken from the request (see newToolDeployment)
                    ToolDeployment savedToolDeployment = toolDeploymentRepository.save(newToolDeployment(toolDeployment, platformDeploymentToChange));
                    platformDeploymentToChange.getToolDeployments().add(savedToolDeployment);
                }
            }
        }

        platformDeploymentRepository.saveAndFlush(platformDeploymentToChange);
        log.info("Updated platform deployment ID: [{}]", id);

        return new ResponseEntity<>(platformDeploymentToChange, HttpStatus.OK);
    }

    // only the LTI deployment ID comes from the request: the rest is a new tool deployment's own state
    private static ToolDeployment newToolDeployment(ToolDeployment requested, PlatformDeployment platformDeployment) {
        return ToolDeployment.builder()
            .ltiDeploymentId(requested.getLtiDeploymentId())
            .platformDeployment(platformDeployment)
            .build();
    }

}
