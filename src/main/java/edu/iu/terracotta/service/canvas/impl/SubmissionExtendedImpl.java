package edu.iu.terracotta.service.canvas.impl;

import com.google.gson.reflect.TypeToken;
import edu.iu.terracotta.service.canvas.SubmissionReaderExtended;
import edu.iu.terracotta.service.canvas.SubmissionWriterExtended;
import edu.ksu.canvas.impl.BaseImpl;
import edu.ksu.canvas.model.assignment.Submission;
import edu.ksu.canvas.net.RestClient;
import edu.ksu.canvas.oauth.OauthToken;
import edu.ksu.canvas.requestOptions.GetSubmissionsOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;

public class SubmissionExtendedImpl extends BaseImpl<Submission, SubmissionReaderExtended, SubmissionWriterExtended>
        implements SubmissionReaderExtended, SubmissionWriterExtended {

    private static final Logger LOG = LoggerFactory.getLogger(SubmissionExtendedImpl.class);


    public SubmissionExtendedImpl(String canvasBaseUrl, Integer apiVersion, OauthToken oauthToken, RestClient restClient,
                                  int connectTimeout, int readTimeout, Integer paginationPageSize, Boolean serializeNulls) {
        super(canvasBaseUrl, apiVersion, oauthToken, restClient, connectTimeout, readTimeout, paginationPageSize, serializeNulls);

    }

    @Override
    protected Type listType() {
        return (new TypeToken<List<edu.ksu.canvas.model.assignment.Submission>>() {
        }).getType();
    }

    @Override
    protected Class<Submission> objectType() {
        return Submission.class;
    }


    @Override
    public List<Submission> listSubmissionsForMultipleAssignments(GetSubmissionsOptions options) throws IOException {
        String url = this.buildCanvasUrl("courses/" + options.getCanvasId() + "/students/submissions", options.getOptionsMap());
        return this.getListFromCanvas(url);
    }


}
