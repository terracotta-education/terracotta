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

package edu.iu.terracotta.utils;

public class TextConstants {




    private TextConstants() {
        throw new IllegalStateException("Utility class");
    }

    public static final String TOKEN = "Token - ";
    public static final String NO_SESSION_VALUES = "noSessionValues";
    public static final String SINGLE = "single";
    public static final String RESULTS = "results";
    public static final String ERROR = "Error";
    public static final String HTML_CONTENT = "htmlContent";
    public static final String LTI3_SUFFIX = "/lti3/";
    public static final String DEFAULT_KID = "OWNKEY";
    public static final String BEARER = "Bearer ";
    public static final String ERROR_DEEP_RESPONSE = "Error creating the DeepLinking Response";
    public static final String NOT_FOUND_SUFFIX = " not found";
    public static final String LTI3ERROR = "lti3Error";
    public static final String DUPLICATE_PREFIX = "Copy of";
    public static final String NOT_ENOUGH_PERMISSIONS = "Error 104: Not enough permissions to access to this endpoint.";
    public static final String BAD_TOKEN = "The token does not contain the expected information";
    public static final String ID_IN_POST_ERROR = "Error 107: Unable to create. The POST endpoint does not accept objects with id";
    public static final String EXPERIMENT_NOT_MATCHING = "Error 108: The experiment does not belong to the course defined in the JWT token.";
    public static final String CONDITION_NOT_MATCHING = "Error 108: The condition does not belong to experiment defined in the path.";
    public static final String EXPOSURE_NOT_MATCHING = "Error 108: The exposure does not belong to experiment defined in the path.";
    public static final String PARTICIPANT_NOT_MATCHING = "Error 108: The participant does not belong to experiment defined in the path.";
    public static final String ASSIGNMENT_NOT_MATCHING = "Error 108: The assignment does not belong to experiment or exposure defined in path.";
    public static final String TREATMENT_NOT_MATCHING = "Error 108: The treatment does not belong to experiment or condition defined in path.";
    public static final String ASSESSMENT_NOT_MATCHING = "Error 108: The assessment does not belong to the experiment, condition, or treatment defined in path.";
    public static final String QUESTION_NOT_MATCHING = "Error 108: The question does not belong to the experiment, condition, treatment, or assignment defined in path.";
    public static final String ANSWER_NOT_MATCHING = "Error 108: The answer does not belong to the experiment, condition, treatment, assignment, or question defined in path.";
    public static final String ANSWER_SUBMISSION_NOT_MATCHING = "Error 108: The answer submission does not belong to the experiment, condition, treatment, assignment, question, or question submission defined in path.";
    public static final String SUBMISSION_NOT_MATCHING = "Error 108: The submission does not belong to the experiment, condition, treatment,or assignment defined in path.";
    public static final String QUESTION_SUBMISSION_NOT_MATCHING = "Error 108: The question submission does not belong to the experiment, condition, treatment, assignment, or submission defined in path.";
    public static final String SUBMISSION_COMMENT_NOT_MATCHING = "Error 108: The question submission does not belong to the experiment, condition, treatment, assignment, or submission defined in path.";
    public static final String QUESTION_SUBMISSION_COMMENT_NOT_MATCHING = "Error 108: The question submission comment does not belong to the experiment, condition, treatment, assignment, submission, or question submission defined in path.";
    public static final String OUTCOME_NOT_MATCHING = "Error 108: The outcome does not belong to the experiment or exposure defined in the path.";
    public static final String OUTCOME_SCORE_NOT_MATCHING = "Error 108: The outcome score does not belong to the experiment, exposure, or outcome defined in the path.";
    public static final String BAD_CONSENT_FILETYPE = "Error 112:The consent must be a pdf file.";
    public static final String ID_MISSING = "Error 125: A valid question id must be included in a submission post.";
    public static final String GROUP_NOT_MATCHING = "Error 108: The group does not belong to experiment defined in the path.";
    public static final String SUBMISSION_IDS_MISSING = "Error 113: The request requires at least one submissionId in the list of submissions";
    public static final String CONSENT_PENDING = "Error 114: Consent_Pending: The student has not signed the consent";
    public static final String GROUP_PENDING = "Error 115: Group_Pending: The student has not been assigned to a group";
    public static final String ASSIGNMENT_LOCKED = "Error 116: The assignment is not open at this moment";
    public static final String LIMIT_OF_SUBMISSIONS_REACHED ="Error 117: You can't answer this assignment again";
    public static final String MAX_SUBMISSION_ATTEMPTS_REACHED = "Error 150: Max submission attempts already reached";
    public static final String ASSIGNMENT_SUBMISSION_WAIT_TIME_NOT_REACHED = "Error 158: You must wait to answer this assignment again";
    public static final String ID_MISMATCH_PUT = "Error 159: IDs do not match in PUT request";
    public static final String NO_ASSIGNMENT_IN_TREATMENTDTO = "Error 129: Unable to update Treatment: The assignmentId is mandatory";
    public static final String NO_CONDITION_FOR_TREATMENT = "The condition for the treatment does not exist";
    public static final String UNABLE_TO_CREATE_TREATMENT = "Error 105: Unable to create Treatment: %s";
    public static final String UNABLE_TO_UPDATE_TREATMENT = "Error 160: Unable to update Treatment: %s";

}
