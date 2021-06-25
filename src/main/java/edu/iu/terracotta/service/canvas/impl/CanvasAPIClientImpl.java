package edu.iu.terracotta.service.canvas.impl;

import edu.iu.terracotta.exceptions.CanvasApiException;
import edu.iu.terracotta.model.PlatformDeployment;
import edu.iu.terracotta.model.canvas.AssignmentExtended;
import edu.iu.terracotta.service.canvas.AssignmentReaderExtended;
import edu.iu.terracotta.service.canvas.AssignmentWriterExtended;
import edu.iu.terracotta.service.canvas.CanvasAPIClient;

import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import edu.ksu.canvas.interfaces.SubmissionReader;
import edu.ksu.canvas.model.assignment.Submission;
import edu.ksu.canvas.requestOptions.GetSingleAssignmentOptions;
import edu.ksu.canvas.requestOptions.GetSubmissionsOptions;
import edu.ksu.canvas.requestOptions.ListCourseAssignmentsOptions;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import edu.ksu.canvas.CanvasApiFactory;
import edu.ksu.canvas.oauth.OauthToken;
import edu.ksu.canvas.oauth.NonRefreshableOauthToken;
import edu.ksu.canvas.model.assignment.Assignment;


@Service
public class CanvasAPIClientImpl implements CanvasAPIClient {


    @Override
    public Optional<AssignmentExtended> createCanvasAssignment(AssignmentExtended canvasAssignment, String contextMembershipUrl, PlatformDeployment platformDeployment ) throws CanvasApiException {
        //https://github.com/kstateome/canvas-api/tree/master

        try {
            String canvasBaseUrl = platformDeployment.getBaseUrl();
            OauthToken oauthToken = new NonRefreshableOauthToken(platformDeployment.getApiToken());
            CanvasApiFactoryExtended apiFactory = new CanvasApiFactoryExtended(canvasBaseUrl);
            AssignmentWriterExtended assignmentWriter = apiFactory.getWriter(AssignmentWriterExtended.class, oauthToken);
            return assignmentWriter.createAssignment(extractCourseId(contextMembershipUrl), canvasAssignment);
        } catch (IOException ex){
            throw new CanvasApiException(
                    "Failed to create Assignment in Canvas course by ID [" + extractCourseId(contextMembershipUrl) + "]", ex);
        }
    }

    @Override
    public List<AssignmentExtended> listAssignments(String contextMembershipUrl, PlatformDeployment platformDeployment) throws CanvasApiException {
        try {
            String canvasBaseUrl = platformDeployment.getBaseUrl();
            OauthToken oauthToken = new NonRefreshableOauthToken(platformDeployment.getApiToken());
            CanvasApiFactoryExtended apiFactory = new CanvasApiFactoryExtended(canvasBaseUrl);
            AssignmentReaderExtended assignmentReader = apiFactory.getReader(AssignmentReaderExtended.class, oauthToken);
            ListCourseAssignmentsOptions listCourseAssignmentsOptions = new ListCourseAssignmentsOptions(extractCourseId(contextMembershipUrl));
            return assignmentReader.listCourseAssignments(listCourseAssignmentsOptions);
        } catch (IOException ex){
            throw new CanvasApiException(
                    "Failed to create Assignment in Canvas course by ID [" + extractCourseId(contextMembershipUrl) + "]", ex);
        }
    }

    @Override
    public List<Submission> listSubmissions(Integer assignmentId, String contextMembershipUrl, PlatformDeployment platformDeployment) throws CanvasApiException, IOException {
        String canvasBaseUrl = platformDeployment.getBaseUrl();
        OauthToken oauthToken = new NonRefreshableOauthToken(platformDeployment.getApiToken());
        CanvasApiFactoryExtended apiFactory = new CanvasApiFactoryExtended(canvasBaseUrl);
        SubmissionReader submissionReader = apiFactory.getReader(SubmissionReader.class,oauthToken);
        GetSubmissionsOptions submissionsOptions = new GetSubmissionsOptions(extractCourseId(contextMembershipUrl),assignmentId);
        submissionsOptions.includes(Collections.singletonList(GetSubmissionsOptions.Include.USER));
        return submissionReader.getCourseSubmissions(submissionsOptions);
    }

    @Override
    public Date getDueAt(String contextMembershipUrl, PlatformDeployment platformDeployment, String lmsAssignmentId) throws IOException, CanvasApiException {
        String canvasBaseUrl = platformDeployment.getBaseUrl();
        OauthToken oauthToken = new NonRefreshableOauthToken(platformDeployment.getApiToken());
        CanvasApiFactoryExtended apiFactory = new CanvasApiFactoryExtended(canvasBaseUrl);
        AssignmentReaderExtended assignmentReader = apiFactory.getReader(AssignmentReaderExtended.class, oauthToken);
        GetSingleAssignmentOptions singleAssignmentOptions = new GetSingleAssignmentOptions(extractCourseId(contextMembershipUrl), Integer.parseInt(lmsAssignmentId));
        Optional<AssignmentExtended> assignmentExtended = assignmentReader.getSingleAssignment(singleAssignmentOptions);
        if (assignmentExtended.isPresent()){
            return assignmentExtended.get().getDueAt();
        } else {
            throw new CanvasApiException("Assignment with id : " + lmsAssignmentId + "can't be retrieved");
        }
    }

    @Override
    public Date getLockAt(String contextMembershipUrl, PlatformDeployment platformDeployment, String lmsAssignmentId) throws IOException, CanvasApiException  {
        String canvasBaseUrl = platformDeployment.getBaseUrl();
        OauthToken oauthToken = new NonRefreshableOauthToken(platformDeployment.getApiToken());
        CanvasApiFactoryExtended apiFactory = new CanvasApiFactoryExtended(canvasBaseUrl);
        AssignmentReaderExtended assignmentReader = apiFactory.getReader(AssignmentReaderExtended.class, oauthToken);
        GetSingleAssignmentOptions singleAssignmentOptions = new GetSingleAssignmentOptions(extractCourseId(contextMembershipUrl), Integer.parseInt(lmsAssignmentId));
        Optional<AssignmentExtended> assignmentExtended = assignmentReader.getSingleAssignment(singleAssignmentOptions);
        if (assignmentExtended.isPresent()){
            return assignmentExtended.get().getLockAt();
        } else {
            throw new CanvasApiException("Assignment with id : " + lmsAssignmentId + "can't be retrieved");
        }
    }

    @Override
    public Date getUnlockAt(String contextMembershipUrl, PlatformDeployment platformDeployment, String lmsAssignmentId) throws IOException, CanvasApiException  {
        String canvasBaseUrl = platformDeployment.getBaseUrl();
        OauthToken oauthToken = new NonRefreshableOauthToken(platformDeployment.getApiToken());
        CanvasApiFactoryExtended apiFactory = new CanvasApiFactoryExtended(canvasBaseUrl);
        AssignmentReaderExtended assignmentReader = apiFactory.getReader(AssignmentReaderExtended.class, oauthToken);
        GetSingleAssignmentOptions singleAssignmentOptions = new GetSingleAssignmentOptions(extractCourseId(contextMembershipUrl), Integer.parseInt(lmsAssignmentId));
        Optional<AssignmentExtended> assignmentExtended = assignmentReader.getSingleAssignment(singleAssignmentOptions);
        if (assignmentExtended.isPresent()){
            return assignmentExtended.get().getUnlockAt();
        } else {
            throw new CanvasApiException("Assignment with id : " + lmsAssignmentId + "can't be retrieved");
        }
    }


    private String extractCourseId (String contextMembershipUrl){
            //TODO deal with exceptions
            return StringUtils.substringBetween(contextMembershipUrl, "courses/", "/names");
    }

}
