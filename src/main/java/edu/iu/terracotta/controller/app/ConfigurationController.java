package edu.iu.terracotta.controller.app;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import edu.iu.terracotta.security.app.roles.LearnerOrHigher;
import edu.iu.terracotta.connectors.generic.dao.model.SecuredInfo;
import edu.iu.terracotta.connectors.generic.exceptions.TerracottaConnectorException;
import edu.iu.terracotta.connectors.generic.service.api.ApiJwtService;
import edu.iu.terracotta.dao.model.dto.ConfigurationDto;
import edu.iu.terracotta.service.app.ConfigurationService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping(value = ConfigurationController.REQUEST_ROOT, produces = MediaType.APPLICATION_JSON_VALUE)
public class ConfigurationController {

    public static final String REQUEST_ROOT = "api/configuration";

    private final ApiJwtService apijwtService;
    private final ConfigurationService configurationService;

    @GetMapping
    @LearnerOrHigher
    public ResponseEntity<ConfigurationDto> get(HttpServletRequest req) throws NumberFormatException, TerracottaConnectorException {
        SecuredInfo securedInfo = apijwtService.extractValues(req, false);

        return new ResponseEntity<>(configurationService.getConfigurations(securedInfo), HttpStatus.OK);
    }

}
