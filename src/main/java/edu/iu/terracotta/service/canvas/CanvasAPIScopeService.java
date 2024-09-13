package edu.iu.terracotta.service.canvas;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import edu.iu.terracotta.exceptions.CanvasAPIScopeNotFoundException;
import edu.iu.terracotta.exceptions.app.FeatureNotFoundException;
import edu.iu.terracotta.model.canvas.CanvasAPIScope;

public interface CanvasAPIScopeService {

    CanvasAPIScope getScopeById(long id) throws CanvasAPIScopeNotFoundException;
    CanvasAPIScope getScopeByUUID(UUID uuid) throws CanvasAPIScopeNotFoundException;
    CanvasAPIScope createScope(CanvasAPIScope canvasAPIScope);
    CanvasAPIScope updateScope(CanvasAPIScope canvasAPIScope);
    void deleteScope(Long id);
    List<String> getScopesForFeature(long featureId);
    String getNecessaryScopes(long plaformDeploymentKeyId, String separator) throws FeatureNotFoundException;
    Set<String> getNecessaryScopes(long plaformDeploymentKeyId) throws FeatureNotFoundException;

}
