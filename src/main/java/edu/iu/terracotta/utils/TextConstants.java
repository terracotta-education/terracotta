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
    public static final String NOT_ENOUGH_PERMISSIONS = "Not enough permissions to access to this endpoint";
    public static final String BAD_TOKEN = "The token does not contain the expected information";
    public static final String ID_IN_POST_ERROR = "Unable to create. The POST endpoint does not accept objects with id";
    public static final String EXPERIMENT_NOT_MATCHING = "The experiment does not belong to the course defined in the JWT token.";
    public static final String CONDITION_NOT_MATCHING = "The condition does not belong to experiment defined in the path.";
    public static final String EXPOSURE_NOT_MATCHING = "The exposure does not belong to experiment defined in the path.";
    public static final String PARTICIPANT_NOT_MATCHING = "The participant does not belong to experiment defined in the path.";
    public static final String BAD_CONSENT_FILETYPE = "The consent must be a pdf file.";

}
