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

package edu.iu.terracotta.controller.app;

import edu.iu.terracotta.exceptions.BadTokenException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import lombok.extern.slf4j.Slf4j;
import edu.iu.terracotta.service.app.APIJWTService;
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
import java.util.List;

@Slf4j
@Controller
@SuppressWarnings({"unchecked", "rawtypes", "PMD.GuardLogStatement"})
@RequestMapping(value = "/api/oauth", produces = MediaType.APPLICATION_JSON_VALUE)
public class TokenController {

    @Autowired private APIJWTService apijwtService;

    @PostMapping("/trade")
    public ResponseEntity getTimedToken(HttpServletRequest req) {
        String token = apijwtService.extractJwtStringValue(req, true);
        Jws<Claims> claims = apijwtService.validateToken(token);

        if ((Boolean) claims.getPayload().get("oneUse")) {
            try {
                // experimentId and assignmentId are optionals so check the null.
                Long assignmentId = null;

                if (claims.getPayload().get("assignmentId") != null) {
                    assignmentId = Long.parseLong(claims.getPayload().get("assignmentId").toString());
                }

                Long experimentId = null;

                if (claims.getPayload().get("experimentId") != null) {
                    experimentId = Long.parseLong(claims.getPayload().get("experimentId").toString());
                }

                return new ResponseEntity<>(apijwtService.buildJwt(false,
                        (List<String>)claims.getPayload().get("roles"),
                        Long.parseLong(claims.getPayload().get("contextId").toString()),
                        Long.parseLong(claims.getPayload().get("platformDeploymentId").toString()),
                        claims.getPayload().get("userId").toString(),
                        assignmentId,
                        experimentId,
                        (Boolean) claims.getPayload().get("consent"),
                        claims.getPayload().get("canvasUserId").toString(),
                        claims.getPayload().get("canvasUserGlobalId").toString(),
                        claims.getPayload().get("canvasLoginId").toString(),
                        claims.getPayload().get("canvasUserName").toString(),
                        claims.getPayload().get("canvasCourseId").toString(),
                        claims.getPayload().get("canvasAssignmentId").toString(),
                        claims.getPayload().get("dueAt").toString(),
                        claims.getPayload().get("lockAt").toString(),
                        claims.getPayload().get("unlockAt").toString(),
                        claims.getPayload().get("nonce").toString(),
                        claims.getPayload().get("allowedAttempts", Integer.class),
                        claims.getPayload().get("studentAttempts", Integer.class))
                        , HttpStatus.OK);
            } catch (GeneralSecurityException | IOException e) {
                return new ResponseEntity<>("Error generating token: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }

            return new ResponseEntity<>("Token passed was not a one time valid token", HttpStatus.UNAUTHORIZED);
    }

    @PostMapping("/refresh")
    public ResponseEntity refreshToken(HttpServletRequest req) {
        String token = apijwtService.extractJwtStringValue(req, true);

        try {
            return new ResponseEntity<>(apijwtService.refreshToken(token), HttpStatus.OK);
        } catch (GeneralSecurityException | IOException e) {
            log.error(e.getMessage(), e);
        } catch (BadTokenException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }

        return new ResponseEntity<>("Error generating token", HttpStatus.INTERNAL_SERVER_ERROR);
    }

}
