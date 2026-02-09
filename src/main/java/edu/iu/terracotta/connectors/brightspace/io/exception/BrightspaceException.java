package edu.iu.terracotta.connectors.brightspace.io.exception;

/**
 * Base exception for errors arising while talking to Brightspace.
 * When thrown, it can optionally carry a string containing the
 * human readable error message returned by Brightspace, if any.
 * Sometimes it may be appropriate to display this error message
 * to the user.It can also carry the URL of the failed request.
 */
public class BrightspaceException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String brightspaceErrorMessage;
    private final String requestUrl;
    private final Object error;

    public BrightspaceException() {
        brightspaceErrorMessage = null;
        requestUrl = null;
        error = null;
    }

    public BrightspaceException(String brightspaceErrorString, String url) {
        super(String.format("Error from URL %s : %s", url, brightspaceErrorString));
        brightspaceErrorMessage = brightspaceErrorString;
        requestUrl = url;
        error = null;
    }

    public BrightspaceException(String brightspaceErrorString, String url, Object error) {
        brightspaceErrorMessage = brightspaceErrorString;
        requestUrl = url;
        this.error = error;
    }

    public Object getError() {
        return error;
    }

    public String getBrightspaceErrorMessage() {
        return brightspaceErrorMessage;
    }

    public String getRequestUrl() {
        return requestUrl;
    }

}
