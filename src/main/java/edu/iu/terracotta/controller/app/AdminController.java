package edu.iu.terracotta.controller.app;

import java.io.IOException;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import edu.iu.terracotta.connectors.generic.dao.model.SecuredInfo;
import edu.iu.terracotta.connectors.generic.exceptions.ApiException;
import edu.iu.terracotta.connectors.generic.exceptions.ConnectionException;
import edu.iu.terracotta.connectors.generic.exceptions.TerracottaConnectorException;
import edu.iu.terracotta.connectors.generic.service.api.ApiJwtService;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.service.app.AdminService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@SuppressWarnings({"PMD.GuardLogStatement"})
@RequestMapping(value = AdminController.REQUEST_ROOT, produces = MediaType.APPLICATION_JSON_VALUE)
public class AdminController {

    public static final String REQUEST_ROOT = "api/platformdeployment/{id}/resync/targeturis";

    @Autowired private ApiJwtService apijwtService;
    @Autowired private AdminService adminService;

    @Deprecated
    @PostMapping
    public ResponseEntity<Void> resyncTargetUris(@PathVariable long id, @RequestBody Map<String, String> options, HttpServletRequest req)
            throws DataServiceException, ConnectionException, IOException, ApiException, NumberFormatException, TerracottaConnectorException {
        SecuredInfo securedInfo = apijwtService.extractValues(req, false);

        if (!apijwtService.isTerracottaAdmin(securedInfo)) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        try {
            adminService.resyncTargetUris(id, options.get("tokenOverride"));
        } catch (ApiException | DataServiceException | ConnectionException | IOException e) {
            log.error("Error resyncing LTI Target URIs with the LMS for Platform Deployment ID: '{}'", id);
        }

        return new ResponseEntity<>(HttpStatus.ACCEPTED);
    }

}
