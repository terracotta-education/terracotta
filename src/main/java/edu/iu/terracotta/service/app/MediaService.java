package edu.iu.terracotta.service.app;

import edu.iu.terracotta.exceptions.NoSubmissionsException;
import edu.iu.terracotta.exceptions.ParameterMissingException;
import edu.iu.terracotta.model.app.dto.media.MediaEventDto;
import edu.iu.terracotta.model.oauth2.SecuredInfo;

public interface MediaService {

    void fromDto(MediaEventDto mediaEventDto, SecuredInfo securedInfo, Long experimentId, Long submissionId, Long questionId) throws ParameterMissingException, NoSubmissionsException;

}
