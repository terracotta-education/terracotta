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
package edu.iu.terracotta.service.lti.impl;

import edu.iu.terracotta.exceptions.ConnectionException;
import edu.iu.terracotta.exceptions.helper.ExceptionMessageGenerator;
import edu.iu.terracotta.model.lti.dto.ToolRegistrationDTO;
import edu.iu.terracotta.service.lti.RegistrationService;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
@SuppressWarnings({"PMD.GuardLogStatement"})
public class RegistrationServiceImpl implements RegistrationService {

    @Autowired
    private ExceptionMessageGenerator exceptionMessageGenerator;

    @Override
    public String callDynamicRegistration(String token, ToolRegistrationDTO toolRegistrationDTO, String endpoint) throws ConnectionException {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + token);

            ResponseEntity<String> registrationRequest = new RestTemplate(new BufferingClientHttpRequestFactory(new SimpleClientHttpRequestFactory())).exchange(
                endpoint,
                HttpMethod.POST,
                new HttpEntity<>(toolRegistrationDTO, headers),
                String.class
            );
            HttpStatusCode status = registrationRequest.getStatusCode();

            if (!status.is2xxSuccessful()) {
                String exceptionMsg = "Can't get confirmation of the registration";
                log.error(exceptionMsg);
                throw new ConnectionException(exceptionMsg);
            }

            return registrationRequest.getBody();
        } catch (Exception e) {
            StringBuilder exceptionMsg = new StringBuilder();
            exceptionMsg.append("Problem during the registration");
            log.error(exceptionMsg.toString(), e);
            throw new ConnectionException(exceptionMessageGenerator.exceptionMessage(exceptionMsg.toString(), e));
        }
    }

}
