package edu.iu.terracotta.connectors.canvas.service.extended.impl;

import com.google.gson.reflect.TypeToken;

import edu.iu.terracotta.connectors.canvas.dao.model.extended.SubmissionExtended;
import edu.iu.terracotta.connectors.canvas.service.extended.SubmissionReaderExtended;
import edu.iu.terracotta.connectors.canvas.service.extended.SubmissionWriterExtended;
import edu.ksu.canvas.impl.BaseImpl;
import edu.ksu.canvas.model.assignment.Submission;
import edu.ksu.canvas.net.Response;
import edu.ksu.canvas.net.RestClient;
import edu.ksu.canvas.oauth.OauthToken;
import edu.ksu.canvas.requestOptions.GetSubmissionsOptions;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class SubmissionExtendedImpl extends BaseImpl<SubmissionExtended, SubmissionReaderExtended, SubmissionWriterExtended> implements SubmissionReaderExtended, SubmissionWriterExtended {

    public SubmissionExtendedImpl(String canvasBaseUrl, Integer apiVersion, OauthToken oauthToken, RestClient restClient, int connectTimeout, int readTimeout, Integer paginationPageSize, Boolean serializeNulls) {
        super(canvasBaseUrl, apiVersion, oauthToken, restClient, connectTimeout, readTimeout, paginationPageSize, serializeNulls);
    }

    @Override
    public List<SubmissionExtended> listSubmissionsForMultipleAssignments(GetSubmissionsOptions options) throws IOException {
        String url = this.buildCanvasUrl(String.format("courses/%s/students/submissions", options.getCanvasId()), options.getOptionsMap());

        return parseList(getListResponseFromCanvas(url));
    }

    @Override
    public List<SubmissionExtended> getCourseSubmissions(GetSubmissionsOptions submissionsOptions) throws IOException {
        String url = buildCanvasUrl(String.format("courses/%s/assignments/%d/submissions", submissionsOptions.getCanvasId(), submissionsOptions.getAssignmentId()), submissionsOptions.getOptionsMap());

        return parseList(getListResponseFromCanvas(url));
    }

    @Override
    protected Type listType() {
        return new TypeToken<List<SubmissionExtended>>() {}.getType();
    }

    @Override
    protected Class<SubmissionExtended> objectType() {
        return SubmissionExtended.class;
    }

    private List<Response> getListResponseFromCanvas(String url) throws IOException {
        Consumer<Response> consumer = null;

        if (responseCallback != null) {
            consumer = response -> responseCallback.accept(responseParser.parseToList(listType(), response));
        }

        List<Response> responses = canvasMessenger.getFromCanvas(oauthToken, url, consumer);
        responseCallback = null;

        return responses;
    }

    private List<SubmissionExtended> parseList(List<Response> responses) {
        List<SubmissionExtended> submissionExtendedList = new ArrayList<>();

        responses.stream()
            .forEach(response -> submissionExtendedList.addAll(parseResponseList(response)));

        return submissionExtendedList;
    }

    private List<SubmissionExtended> parseResponseList(Response response) {
        List<SubmissionExtended> submissionExtendedList = responseParser.parseToList(
                listType(),
                response
            );
        List<Submission> submissionList = responseParser.parseToList(
            new TypeToken<List<Submission>>() {}.getType(),
            response
        );

        AtomicInteger index = new AtomicInteger(0);

        return submissionExtendedList.stream()
            .map(
                submissionExtended -> {
                    submissionExtended.setSubmission(submissionList.get(index.getAndIncrement()));
                    submissionExtended.setType(Submission.class);

                    return submissionExtended;
                }
            )
            .toList();
    }

}
