package edu.iu.terracotta.service.canvas.impl;

import edu.iu.terracotta.exceptions.CanvasApiException;
import edu.iu.terracotta.model.PlatformDeployment;
import edu.iu.terracotta.model.canvas.AssignmentExtended;
import edu.iu.terracotta.service.canvas.AssignmentReaderExtended;
import edu.iu.terracotta.service.canvas.AssignmentWriterExtended;
import edu.iu.terracotta.service.canvas.CanvasAPIClient;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import edu.ksu.canvas.interfaces.SubmissionReader;
import edu.ksu.canvas.model.assignment.Submission;
import edu.ksu.canvas.requestOptions.GetSingleAssignmentOptions;
import edu.ksu.canvas.requestOptions.GetSubmissionsOptions;
import edu.ksu.canvas.requestOptions.ListCourseAssignmentsOptions;
import org.springframework.stereotype.Service;

import edu.ksu.canvas.oauth.OauthToken;
import edu.ksu.canvas.oauth.NonRefreshableOauthToken;


@Service
public class CanvasAPIClientImpl implements CanvasAPIClient {


    @Override
    public Optional<AssignmentExtended> createCanvasAssignment(AssignmentExtended canvasAssignment, String canvasCourseId, PlatformDeployment platformDeployment ) throws CanvasApiException {
        //https://github.com/kstateome/canvas-api/tree/master

        try {
            String canvasBaseUrl = platformDeployment.getBaseUrl();
            OauthToken oauthToken = new NonRefreshableOauthToken(platformDeployment.getApiToken());
            CanvasApiFactoryExtended apiFactory = new CanvasApiFactoryExtended(canvasBaseUrl);
            AssignmentWriterExtended assignmentWriter = apiFactory.getWriter(AssignmentWriterExtended.class, oauthToken);
            return assignmentWriter.createAssignment(canvasCourseId, canvasAssignment);
        } catch (IOException ex){
            throw new CanvasApiException(
                    "Failed to create Assignment in Canvas course by ID [" + canvasCourseId + "]", ex);
        }
    }

    @Override
    public List<AssignmentExtended> listAssignments(String canvasCourseId, PlatformDeployment platformDeployment) throws CanvasApiException {
        try {
            String canvasBaseUrl = platformDeployment.getBaseUrl();
            OauthToken oauthToken = new NonRefreshableOauthToken(platformDeployment.getApiToken());
            CanvasApiFactoryExtended apiFactory = new CanvasApiFactoryExtended(canvasBaseUrl);
            AssignmentReaderExtended assignmentReader = apiFactory.getReader(AssignmentReaderExtended.class, oauthToken);
            ListCourseAssignmentsOptions listCourseAssignmentsOptions = new ListCourseAssignmentsOptions(canvasCourseId);
            return assignmentReader.listCourseAssignments(listCourseAssignmentsOptions);
        } catch (IOException ex){
            throw new CanvasApiException(
                    "Failed to get the list of assignments Canvas course [" + canvasCourseId + "]", ex);
        }
    }

    @Override
    public List<Submission> listSubmissions(Integer assignmentId, String canvasCourseId, PlatformDeployment platformDeployment) throws CanvasApiException, IOException {
        String canvasBaseUrl = platformDeployment.getBaseUrl();
        OauthToken oauthToken = new NonRefreshableOauthToken(platformDeployment.getApiToken());
        CanvasApiFactoryExtended apiFactory = new CanvasApiFactoryExtended(canvasBaseUrl);
        SubmissionReader submissionReader = apiFactory.getReader(SubmissionReader.class,oauthToken);
        GetSubmissionsOptions submissionsOptions = new GetSubmissionsOptions(canvasCourseId,assignmentId);
        submissionsOptions.includes(Collections.singletonList(GetSubmissionsOptions.Include.USER));
        return submissionReader.getCourseSubmissions(submissionsOptions);
    }

    @Override
    public boolean checkAssignmentExists(Integer assignmentId, String canvasCourseId, PlatformDeployment platformDeployment) throws CanvasApiException {
        try {
            String canvasBaseUrl = platformDeployment.getBaseUrl();
            OauthToken oauthToken = new NonRefreshableOauthToken(platformDeployment.getApiToken());
            CanvasApiFactoryExtended apiFactory = new CanvasApiFactoryExtended(canvasBaseUrl);
            AssignmentReaderExtended assignmentReader = apiFactory.getReader(AssignmentReaderExtended.class, oauthToken);
            GetSingleAssignmentOptions getSingleAssignmentsOptions = new GetSingleAssignmentOptions(canvasCourseId, assignmentId);
            return (assignmentReader.getSingleAssignment(getSingleAssignmentsOptions).isPresent());
        } catch (IOException ex){
            throw new CanvasApiException(
                    "Failed to get the Assignment in Canvas course by ID [" + canvasCourseId + "]", ex);
        }
    }

}
