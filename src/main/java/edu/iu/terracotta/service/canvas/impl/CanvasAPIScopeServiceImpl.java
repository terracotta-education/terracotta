package edu.iu.terracotta.service.canvas.impl;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import edu.iu.terracotta.exceptions.CanvasAPIScopeNotFoundException;
import edu.iu.terracotta.model.canvas.CanvasAPIScope;
import edu.iu.terracotta.repository.CanvasAPIScopeRepository;
import edu.iu.terracotta.service.canvas.CanvasAPIScopeService;

@Service
public class CanvasAPIScopeServiceImpl implements CanvasAPIScopeService {

    @Autowired private CanvasAPIScopeRepository canvasAPIScopeRepository;

    @Override
    public List<CanvasAPIScope> getAllScopes() {
        return canvasAPIScopeRepository.findAll();
    }

    @Override
    public List<CanvasAPIScope> getScopesByRequired(boolean required) {
        return canvasAPIScopeRepository.findByRequired(required);
    }

    @Override
    public CanvasAPIScope getScopeById(long id) throws CanvasAPIScopeNotFoundException {
        return canvasAPIScopeRepository
            .findById(id)
            .orElseThrow(
                () -> new CanvasAPIScopeNotFoundException(String.format("No Canvas API Scope found for ID: [%s] ", id))
            );
    }

    @Override
    public CanvasAPIScope getScopeByUUID(UUID uuid)  throws CanvasAPIScopeNotFoundException {
        return canvasAPIScopeRepository
            .findByUuid(uuid)
            .orElseThrow(
                () -> new CanvasAPIScopeNotFoundException(String.format("No Canvas API Scope found for UUID: [%s] ", uuid))
            );
    }

    @Override
    public CanvasAPIScope createScope(CanvasAPIScope canvasAPIScope) {
        return canvasAPIScopeRepository.save(canvasAPIScope);
    }

    @Override
    public CanvasAPIScope updateScope(CanvasAPIScope canvasAPIScope) {
        return canvasAPIScopeRepository.save(canvasAPIScope);
    }

    @Override
    public void deleteScope(Long id) {
        canvasAPIScopeRepository.deleteById(id);
    }

    @Override
    public List<String> getAllScopeValues() {
        return getAllScopes()
            .stream()
            .map(CanvasAPIScope::getScope)
            .toList();
    }

    @Override
    public List<String> getRequiredScopeValues(boolean required) {
        return getScopesByRequired(required)
            .stream()
            .map(CanvasAPIScope::getScope)
            .toList();
    }

}
