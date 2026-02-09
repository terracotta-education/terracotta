package edu.iu.terracotta.connectors.brightspace.io.exception;

/**
 * Thrown if Brightspace returns a 404 response.
 */
public class ObjectNotFoundException extends BrightspaceException {

    private static final long serialVersionUID = 1L;

    public ObjectNotFoundException() {
        super();
    }

    public ObjectNotFoundException(String brightspaceErrorString, String url) {
        super(brightspaceErrorString, url);
    }

}
