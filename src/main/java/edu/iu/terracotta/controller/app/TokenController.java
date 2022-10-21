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
import edu.iu.terracotta.service.app.APIJWTService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;

@Controller
@RequestMapping(value = "/api/oauth", produces = MediaType.APPLICATION_JSON_VALUE)
public class TokenController {

    @Autowired
    private APIJWTService apijwtService;

    @SuppressWarnings("rawtypes")
    @PostMapping("/trade")
    public ResponseEntity getTimedToken(HttpServletRequest req) {
        //TODO, surely we want to do something more complicated here. Like read the previous token values and make the token similar
        String token = apijwtService.extractJwtStringValue(req, true);
        Jws<Claims> claims = apijwtService.validateToken(token);

        if (!(Boolean)claims.getBody().get("oneUse")) {
            return new ResponseEntity<>("Token passed was not a one time valid token", HttpStatus.UNAUTHORIZED);
        }

        try {
            //ExperimentId and assignmentId are optionals so I need to check the null.
            Long assignmentId = null;
            if (claims.getBody().get("assignmentId")!=null){
                assignmentId = Long.parseLong(claims.getBody().get("assignmentId").toString());
            }
            Long experimentId = null;
            if (claims.getBody().get("experimentId")!=null){
                experimentId = Long.parseLong(claims.getBody().get("experimentId").toString());
            }
            return new ResponseEntity<>(apijwtService.buildJwt(false,
                    (List<String>)claims.getBody().get("roles"),
                    Long.parseLong(claims.getBody().get("contextId").toString()),
                    Long.parseLong(claims.getBody().get("platformDeploymentId").toString()),
                    claims.getBody().get("userId").toString(),
                    assignmentId,
                    experimentId,
                    (Boolean) claims.getBody().get("consent"),
                    claims.getBody().get("canvasUserId").toString(),
                    claims.getBody().get("canvasUserGlobalId").toString(),
                    claims.getBody().get("canvasLoginId").toString(),
                    claims.getBody().get("canvasUserName").toString(),
                    claims.getBody().get("canvasCourseId").toString(),
                    claims.getBody().get("canvasAssignmentId").toString(),
                    claims.getBody().get("dueAt").toString(),
                    claims.getBody().get("lockAt").toString(),
                    claims.getBody().get("unlockAt").toString(),
                    claims.getBody().get("nonce").toString())
                    , HttpStatus.OK);
        } catch (GeneralSecurityException | IOException e) {
            return new ResponseEntity<>("Error generating token: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @SuppressWarnings("rawtypes")
    @PostMapping("/refresh")
    public ResponseEntity refreshToken(HttpServletRequest req) {
        String token = apijwtService.extractJwtStringValue(req, true);

        try {
            return new ResponseEntity<>(apijwtService.refreshToken(token), HttpStatus.OK);
        } catch (GeneralSecurityException | IOException e) {
            e.printStackTrace();
            return new ResponseEntity<>("Error generating token", HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (BadTokenException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

}
