package edu.iu.terracotta.config;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;

import jakarta.persistence.EntityManager;

/**
 * {@link ContextProvider#context} is a static field private to this class (a separate field
 * from {@link ApplicationConfig}'s and {@link SpringContext}'s statics, confirmed by inspection
 * of the production source), but it is still shared across every test method run in the same
 * JVM. Each test below calls {@code setApplicationContext(...)} as its own arrange step so it is
 * self-contained regardless of execution order.
 */
@ExtendWith(MockitoExtension.class)
public class ContextProviderTest {

    @Mock private ApplicationContext applicationContext;

    private ContextProvider contextProvider;

    @BeforeEach
    void beforeEach() {
        contextProvider = new ContextProvider();
    }

    @Test
    void testGetBean() {
        contextProvider.setApplicationContext(applicationContext);
        SomeBean bean = mock(SomeBean.class);
        when(applicationContext.getBean(SomeBean.class)).thenReturn(bean);

        assertSame(bean, ContextProvider.getBean(SomeBean.class));
    }

    @Test
    void testGetBeanDifferentType() {
        contextProvider.setApplicationContext(applicationContext);
        AnotherBean bean = mock(AnotherBean.class);
        when(applicationContext.getBean(AnotherBean.class)).thenReturn(bean);

        assertSame(bean, ContextProvider.getBean(AnotherBean.class));
    }

    @Test
    void testGetEntityManager() {
        contextProvider.setApplicationContext(applicationContext);
        EntityManager entityManager = mock(EntityManager.class);
        when(applicationContext.getBean(EntityManager.class)).thenReturn(entityManager);

        assertSame(entityManager, ContextProvider.getEntityManager());
    }

    private static class SomeBean {
    }

    private static class AnotherBean {
    }

}
