package edu.iu.terracotta.service.canvas.impl;

import edu.iu.terracotta.exceptions.CanvasApiException;
import edu.iu.terracotta.model.PlatformDeployment;
import edu.iu.terracotta.service.canvas.CanvasAPIClient;

import java.io.IOException;
import java.util.Optional;

import edu.ksu.canvas.interfaces.AssignmentWriter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import edu.ksu.canvas.CanvasApiFactory;
import edu.ksu.canvas.oauth.OauthToken;
import edu.ksu.canvas.oauth.NonRefreshableOauthToken;
import edu.ksu.canvas.model.assignment.Assignment;


@Service
public class CanvasAPIClientImpl implements CanvasAPIClient {


    @Override
    public Optional<Assignment> createCanvasAssignment(Assignment canvasAssignment, String contextMembershipUrl, PlatformDeployment platformDeployment ) throws CanvasApiException {
        //https://github.com/kstateome/canvas-api/tree/master

        try {
            String canvasBaseUrl = platformDeployment.getBaseUrl();
            OauthToken oauthToken = new NonRefreshableOauthToken(platformDeployment.getApiToken());
            CanvasApiFactory apiFactory = new CanvasApiFactory(canvasBaseUrl);
            AssignmentWriter assignmentWriter = apiFactory.getWriter(AssignmentWriter.class, oauthToken);
            Optional<Assignment> assignment = assignmentWriter.createAssignment(extractCourseId(contextMembershipUrl), canvasAssignment);
            return assignment;
        } catch (IOException ex){
            throw new CanvasApiException(
                    "Failed to create Assignment in Canvas course by ID [" + extractCourseId(contextMembershipUrl) + "]", ex);
        }
    }

    private String extractCourseId (String contextMembershipUrl){
            //TODO deal with exceptions
            return StringUtils.substringBetween(contextMembershipUrl, "courses/", "/names");
    }

}
