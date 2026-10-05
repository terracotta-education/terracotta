package edu.iu.terracotta.dao.model.distribute;

import java.util.HashMap;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Which copied LMS assignments a copy candidate's recreation re-points, by LMS assignment ID -
 * saved (as JSON, on the candidate) the first time recreation matches them by launch URL, so a
 * retry can find them again even after an interrupted attempt already changed their URLs.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RepointPlan {

    // source (old) Assignment ID -> LMS assignment ID
    @Builder.Default
    private Map<Long, String> assignments = new HashMap<>();

    // LMS assignment ID of the copied consent assignment, if any
    private String consentAssignment;

}
