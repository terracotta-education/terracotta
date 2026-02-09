package edu.iu.terracotta.dao.exceptions;

public class AssignmentNotCreatedException extends Exception {

    public AssignmentNotCreatedException(String message) {
        super(message);
    }

    public AssignmentNotCreatedException(String message, Throwable cause) {
        super(message, cause);
    }

}
