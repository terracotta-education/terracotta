package edu.iu.terracotta.connectors.generic.dao.entity.lti;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Tests {@link PlatformDeployment#getLocalUrl()}, a hand-written override of the Lombok-generated
 * getter for the {@code localUrl} field. It returns the static {@link PlatformDeployment#LOCAL_URL}
 * constant when {@code localUrl} is blank, otherwise the field's actual value.
 */
public class PlatformDeploymentTest {

    @Test
    public void testGetLocalUrlReturnsConstantWhenFieldIsNull() {
        PlatformDeployment platformDeployment = PlatformDeployment.builder()
            .build();

        assertEquals(PlatformDeployment.LOCAL_URL, platformDeployment.getLocalUrl());
    }

    @Test
    public void testGetLocalUrlReturnsConstantWhenFieldIsBlank() {
        PlatformDeployment platformDeployment = PlatformDeployment.builder()
            .localUrl("   ")
            .build();

        assertEquals(PlatformDeployment.LOCAL_URL, platformDeployment.getLocalUrl());
    }

    @Test
    public void testGetLocalUrlReturnsConstantWhenFieldIsEmpty() {
        PlatformDeployment platformDeployment = PlatformDeployment.builder()
            .localUrl("")
            .build();

        assertEquals(PlatformDeployment.LOCAL_URL, platformDeployment.getLocalUrl());
    }

    @Test
    public void testGetLocalUrlReturnsFieldValueWhenSet() {
        PlatformDeployment platformDeployment = PlatformDeployment.builder()
            .localUrl("https://my-real-local-url.example.com")
            .build();

        assertEquals("https://my-real-local-url.example.com", platformDeployment.getLocalUrl());
    }

}
