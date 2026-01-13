package edu.iu.terracotta.service.app.preview;

import java.util.UUID;

import edu.iu.terracotta.connectors.generic.dao.model.SecuredInfo;
import edu.iu.terracotta.dao.entity.preview.TreatmentPreview;
import edu.iu.terracotta.dao.exceptions.AssessmentNotMatchingException;
import edu.iu.terracotta.dao.exceptions.TreatmentNotMatchingException;
import edu.iu.terracotta.dao.model.dto.preview.TreatmentPreviewDto;

public interface TreatmentPreviewService {

    TreatmentPreview create(long treatmentId, long experimentId, long conditionId, String ownerId);
    TreatmentPreviewDto getTreatmentPreview(UUID uuid, long treatmentId, long experimentId, long conditionId, String ownerId, SecuredInfo securedInfo) throws TreatmentNotMatchingException, AssessmentNotMatchingException;

}
