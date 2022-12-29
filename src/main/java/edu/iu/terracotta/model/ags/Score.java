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
package edu.iu.terracotta.model.ags;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class Score {

    @JsonProperty
    private String userId;

    @JsonProperty
    private String scoreMaximum;

    @JsonProperty
    private String scoreGiven;

    @JsonProperty
    private String comment;

    @JsonProperty
    private String activityProgress;

    @JsonProperty
    private String gradingProgress;

    @JsonProperty
    private String timestamp;

    @JsonProperty("https://canvas.instructure.com/lti/submission")
    private Map<String, Object> canvasSubmissionExtension = new HashMap<>();

}
