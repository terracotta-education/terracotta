package edu.iu.terracotta.service.canvas;

import java.util.List;
import java.util.UUID;

import edu.iu.terracotta.exceptions.CanvasAPIScopeNotFoundException;
import edu.iu.terracotta.model.canvas.CanvasAPIScope;

public interface CanvasAPIScopeService {

    List<CanvasAPIScope> getAllScopes();
    List<CanvasAPIScope> getScopesByRequired(boolean required);
    CanvasAPIScope getScopeById(long id) throws CanvasAPIScopeNotFoundException;
    CanvasAPIScope getScopeByUUID(UUID uuid) throws CanvasAPIScopeNotFoundException;
    CanvasAPIScope createScope(CanvasAPIScope canvasAPIScope);
    CanvasAPIScope updateScope(CanvasAPIScope canvasAPIScope);
    void deleteScope(Long id);
    List<String> getAllScopeValues();
    List<String> getRequiredScopeValues(boolean required);

}
