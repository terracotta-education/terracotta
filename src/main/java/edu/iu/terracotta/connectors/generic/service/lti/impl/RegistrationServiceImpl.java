package edu.iu.terracotta.connectors.generic.service.lti.impl;

import edu.iu.terracotta.connectors.generic.dao.model.lti.dto.ToolRegistrationDto;
import edu.iu.terracotta.connectors.generic.exceptions.ConnectionException;
import edu.iu.terracotta.connectors.generic.exceptions.helper.ExceptionMessageGenerator;
import edu.iu.terracotta.connectors.generic.service.lti.RegistrationService;
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

    @Autowired private ExceptionMessageGenerator exceptionMessageGenerator;

    @Override
    public String callDynamicRegistration(String token, ToolRegistrationDto toolRegistrationDto, String endpoint) throws ConnectionException {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + token);

            ResponseEntity<String> registrationRequest = createRestTemplate().exchange(
                endpoint,
                HttpMethod.POST,
                new HttpEntity<>(toolRegistrationDto, headers),
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

    public RestTemplate createRestTemplate() {
        return new RestTemplate(new BufferingClientHttpRequestFactory(new SimpleClientHttpRequestFactory()));
    }
}
