package edu.iu.terracotta.dao.model.dto.distribute;

import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Every ExperimentCopyCandidate currently PENDING for the caller's context is resolved in one
 * action: candidates named here are imported (and their corresponding copied LMS assignment(s)
 * re-pointed); every other PENDING candidate for that context is declined and obsolete-processed.
 * "Declined" is computed server-side against the live PENDING set, not trusted from this list.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CopyCandidateResolutionRequestDto {

    private List<UUID> importCandidateIds;

}
