package edu.iu.terracotta.connectors.canvas.service.extended.impl;

import com.google.gson.reflect.TypeToken;

import edu.iu.terracotta.connectors.canvas.dao.model.extended.SubmissionExtended;
import edu.iu.terracotta.connectors.canvas.service.extended.SubmissionReaderExtended;
import edu.iu.terracotta.connectors.canvas.service.extended.SubmissionWriterExtended;
import edu.ksu.canvas.impl.BaseImpl;
import edu.ksu.canvas.impl.SubmissionImpl;
import edu.ksu.canvas.model.assignment.Submission;
import edu.ksu.canvas.net.RestClient;
import edu.ksu.canvas.oauth.OauthToken;
import edu.ksu.canvas.requestOptions.GetSubmissionsOptions;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class SubmissionExtendedImpl extends BaseImpl<SubmissionExtended, SubmissionReaderExtended, SubmissionWriterExtended> implements SubmissionReaderExtended, SubmissionWriterExtended {

    private SubmissionImpl submissionImpl = null;

    public SubmissionExtendedImpl(String canvasBaseUrl, Integer apiVersion, OauthToken oauthToken, RestClient restClient, int connectTimeout, int readTimeout, Integer paginationPageSize, Boolean serializeNulls) {
        super(canvasBaseUrl, apiVersion, oauthToken, restClient, connectTimeout, readTimeout, paginationPageSize, serializeNulls);
        this.submissionImpl = new SubmissionImpl(canvasBaseUrl, apiVersion, oauthToken, restClient, connectTimeout, readTimeout, paginationPageSize, serializeNulls);
    }

    @Override
    public List<SubmissionExtended> listSubmissionsForMultipleAssignments(GetSubmissionsOptions options) throws IOException {
        return parseList(submissionImpl.listCourseSubmissionsForMultipleAssignments(options));
    }

    @Override
    public List<SubmissionExtended> getCourseSubmissions(GetSubmissionsOptions submissionsOptions) throws IOException {
        return parseList(submissionImpl.getCourseSubmissions(submissionsOptions));
    }

    @Override
    protected Type listType() {
        return new TypeToken<List<SubmissionExtended>>() {}.getType();
    }

    @Override
    protected Class<SubmissionExtended> objectType() {
        return SubmissionExtended.class;
    }

    private List<SubmissionExtended> parseList(List<Submission> submissions) {
        List<SubmissionExtended> submissionExtendedList = new ArrayList<>();

        submissions.stream()
            .forEach(
                submission -> submissionExtendedList.add(
                    SubmissionExtended.builder()
                        .submission(submission)
                        .type(Submission.class)
                        .build()
                )
            );

            return submissionExtendedList;
    }

}
