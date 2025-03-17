package edu.iu.terracotta.controller.app;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import edu.iu.terracotta.connectors.generic.dao.model.SecuredInfo;
import edu.iu.terracotta.connectors.generic.exceptions.TerracottaConnectorException;
import edu.iu.terracotta.connectors.generic.service.api.ApiJwtService;
import edu.iu.terracotta.dao.model.dto.ConfigurationDto;
import edu.iu.terracotta.service.app.ConfigurationService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@SuppressWarnings({"PMD.GuardLogStatement"})
@RequestMapping(value = ConfigurationController.REQUEST_ROOT, produces = MediaType.APPLICATION_JSON_VALUE)
public class ConfigurationController {

    public static final String REQUEST_ROOT = "api/configuration";

    @Autowired private ApiJwtService apijwtService;
    @Autowired private ConfigurationService configurationService;

    @GetMapping
    public ResponseEntity<ConfigurationDto> get(HttpServletRequest req) throws NumberFormatException, TerracottaConnectorException {
        SecuredInfo securedInfo = apijwtService.extractValues(req, false);

        if (!apijwtService.isInstructorOrHigher(securedInfo)) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        return new ResponseEntity<>(configurationService.getConfigurations(), HttpStatus.OK);
    }

}
