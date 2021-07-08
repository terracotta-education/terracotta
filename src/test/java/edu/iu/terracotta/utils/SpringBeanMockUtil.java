package edu.iu.terracotta.utils;
import org.mockito.Mockito;
import org.springframework.aop.framework.Advised;
import org.springframework.aop.support.AopUtils;
import org.springframework.test.util.ReflectionTestUtils;

@SuppressWarnings("unchecked")
public class SpringBeanMockUtil {
    /**
     * https://stackoverflow.com/questions/12857981/transactional-annotation-avoids-services-being-mocked
     * If the given object is a proxy, set the return value as the object being proxied, otherwise return the given
     * object.
     */
    private static <T> T unwrapProxy(T bean) {
        try {
            if (AopUtils.isAopProxy(bean) && bean instanceof Advised) {
                Advised advised = (Advised) bean;
                bean = (T) advised.getTargetSource().getTarget();
            }
            return bean;
        }
        catch (Exception e) {
            throw new RuntimeException("Could not unwrap proxy!", e);
        }
    }

    public static <T> T mockFieldOnBean(Object beanToInjectMock, Class<T> classToMock) {
        T mocked = Mockito.mock(classToMock);
        ReflectionTestUtils.setField(unwrapProxy(beanToInjectMock), null, mocked, classToMock);
        return mocked;
    }
}