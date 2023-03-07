/**
 * Copyright 2021 Unicon (R)
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.iu.terracotta.controller.lti;

import edu.iu.terracotta.repository.PlatformDeploymentRepository;
import edu.iu.terracotta.repository.ToolDeploymentRepository;
import edu.iu.terracotta.model.PlatformDeployment;
import edu.iu.terracotta.model.ToolDeployment;
import edu.iu.terracotta.utils.TextConstants;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
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

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Optional;

/**
 * This controller is protected by basic authentication
 * Allows to read and change the configuration
 */
@Slf4j
@Controller
@Scope("session")
@RequestMapping("/config")
public class ConfigurationController {

    @Autowired
    private PlatformDeploymentRepository platformDeploymentRepository;

    @Autowired
    private ToolDeploymentRepository toolDeploymentRepository;

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
        log.info("Creating Deployment : {}", platformDeployment);

        if (!platformDeploymentRepository.findByIssAndClientId(platformDeployment.getIss(), platformDeployment.getClientId()).isEmpty()) {
            log.error("Unable to create. A platformDeployment like that already exist");
            return new ResponseEntity<String>("Unable to create. A platformDeployment with same key already exist.", HttpStatus.CONFLICT);
        }

        PlatformDeployment platformDeploymentSaved = platformDeploymentRepository.save(platformDeployment);

        if (platformDeployment.getToolDeployments() != null) {
            for (ToolDeployment toolDeployment : platformDeployment.getToolDeployments()) {
                toolDeployment.setPlatformDeployment(platformDeploymentSaved);
                toolDeploymentRepository.save(toolDeployment);
            }
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(ucBuilder.path("/config/{id}").buildAndExpand(platformDeploymentSaved.getKeyId()).toUri());

        return new ResponseEntity<>(headers, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateDeployment(@PathVariable("id") long id, @RequestBody PlatformDeployment platformDeployment) {
        log.info("Updating User with id {}", id);
        Optional<PlatformDeployment> platformDeploymentSearchResult = platformDeploymentRepository.findById(id);

        if (!platformDeploymentSearchResult.isPresent()) {
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
        for (ToolDeployment toolDeployment : platformDeployment.getToolDeployments()) {
            if (platformDeploymentToChange.getToolDeployments().stream()
                    .noneMatch(td -> td.getLtiDeploymentId().equals(toolDeployment.getLtiDeploymentId()))) {
                toolDeployment.setPlatformDeployment(platformDeploymentToChange);
                ToolDeployment savedToolDeployment = toolDeploymentRepository.save(toolDeployment);
                platformDeploymentToChange.getToolDeployments().add(savedToolDeployment);
            }
        }

        platformDeploymentRepository.saveAndFlush(platformDeploymentToChange);

        return new ResponseEntity<>(platformDeploymentToChange, HttpStatus.OK);
    }

}
