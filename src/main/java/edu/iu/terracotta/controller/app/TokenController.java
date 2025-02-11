package edu.iu.terracotta.controller.app;

import edu.iu.terracotta.connectors.generic.exceptions.TerracottaConnectorException;
import edu.iu.terracotta.connectors.generic.service.api.ApiJwtService;
import edu.iu.terracotta.exceptions.BadTokenException;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.security.GeneralSecurityException;

@Slf4j
@Controller
@SuppressWarnings({"rawtypes", "PMD.GuardLogStatement"})
@RequestMapping(value = "/api/oauth", produces = MediaType.APPLICATION_JSON_VALUE)
public class TokenController {

    @Autowired private ApiJwtService apiJwtService;

    @PostMapping("/trade")
    public ResponseEntity getTimedToken(HttpServletRequest req) throws NumberFormatException, TerracottaConnectorException {
        return apiJwtService.getTimedToken(req);
    }

    @PostMapping("/refresh")
    public ResponseEntity refreshToken(HttpServletRequest req) throws NumberFormatException, TerracottaConnectorException {
        String token = apiJwtService.extractJwtStringValue(req, true);

        try {
            return new ResponseEntity<>(apiJwtService.refreshToken(token), HttpStatus.OK);
        } catch (GeneralSecurityException | IOException e) {
            log.error(e.getMessage(), e);
        } catch (BadTokenException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }

        return new ResponseEntity<>("Error generating token", HttpStatus.INTERNAL_SERVER_ERROR);
    }

}
