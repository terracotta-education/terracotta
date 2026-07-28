package edu.iu.terracotta.service.app.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import edu.iu.terracotta.base.BaseTest;
import edu.iu.terracotta.dao.entity.Feature;
import edu.iu.terracotta.dao.model.dto.FeatureDto;
import edu.iu.terracotta.dao.model.enums.FeatureType;
import edu.iu.terracotta.dao.repository.FeatureRepository;

public class FeatureServiceImplTest extends BaseTest {

    @InjectMocks private FeatureServiceImpl featureService;

    @Mock private FeatureRepository featureRepository;
    @Mock private Feature messagingFeature;

    @BeforeEach
    public void beforeEach() {
        MockitoAnnotations.openMocks(this);

        setup();

        when(feature.getType()).thenReturn(FeatureType.DEFAULT);
        when(messagingFeature.getType()).thenReturn(FeatureType.MESSAGING);
    }

    @Test
    public void testToDtoFeature() {
        FeatureDto dto = featureService.toDto(feature);

        assertNotNull(dto);
        assertEquals(FeatureType.DEFAULT, dto.getType());
    }

    @Test
    public void testToDtoFeatureList() {
        List<FeatureDto> dtos = featureService.toDto(List.of(feature, messagingFeature));

        assertEquals(2, dtos.size());
        assertEquals(FeatureType.DEFAULT, dtos.get(0).getType());
        assertEquals(FeatureType.MESSAGING, dtos.get(1).getType());
    }

    @Test
    public void testToDtoFeatureListEmpty() {
        List<FeatureDto> dtos = featureService.toDto(Collections.emptyList());

        assertTrue(dtos.isEmpty());
    }

    @Test
    public void testIsFeatureEnabledTrue() {
        when(featureRepository.findAllByPlatformDeployments_KeyId(anyLong())).thenReturn(List.of(messagingFeature));

        assertTrue(featureService.isFeatureEnabled(FeatureType.MESSAGING, 1L));
    }

    @Test
    public void testIsFeatureEnabledFalseNoMatch() {
        when(featureRepository.findAllByPlatformDeployments_KeyId(anyLong())).thenReturn(List.of(feature));

        assertFalse(featureService.isFeatureEnabled(FeatureType.MESSAGING, 1L));
    }

    @Test
    public void testIsFeatureEnabledFalseEmpty() {
        when(featureRepository.findAllByPlatformDeployments_KeyId(anyLong())).thenReturn(Collections.emptyList());

        assertFalse(featureService.isFeatureEnabled(FeatureType.MESSAGING, 1L));
    }

}
