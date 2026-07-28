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

/**
 * {@link SpringContext#context} is a static field shared across every test method run in the
 * same JVM (it is a field private to this class, separate from {@link ApplicationConfig}'s and
 * {@link ContextProvider}'s own independent static {@code context} fields). Each test below
 * calls {@code setApplicationContext(...)} as its own arrange step so it is self-contained
 * regardless of execution order.
 */
@ExtendWith(MockitoExtension.class)
public class SpringContextTest {

    @Mock private ApplicationContext applicationContext;

    private SpringContext springContext;

    @BeforeEach
    void beforeEach() {
        springContext = new SpringContext();
    }

    @Test
    void testGetBeanFirstType() {
        springContext.setApplicationContext(applicationContext);
        FirstBean bean = mock(FirstBean.class);
        when(applicationContext.getBean(FirstBean.class)).thenReturn(bean);

        assertSame(bean, SpringContext.getBean(FirstBean.class));
    }

    @Test
    void testGetBeanSecondType() {
        springContext.setApplicationContext(applicationContext);
        SecondBean bean = mock(SecondBean.class);
        when(applicationContext.getBean(SecondBean.class)).thenReturn(bean);

        assertSame(bean, SpringContext.getBean(SecondBean.class));
    }

    private static class FirstBean {
    }

    private static class SecondBean {
    }

}
