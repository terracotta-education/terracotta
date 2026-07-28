package edu.iu.terracotta.connectors.generic.service.lms.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import edu.iu.terracotta.base.BaseTest;
import edu.iu.terracotta.connectors.generic.exceptions.TerracottaConnectorException;
import edu.iu.terracotta.connectors.generic.service.connector.ConnectorService;
import edu.iu.terracotta.connectors.generic.service.lms.LmsUtils;

public class LmsUtilsImplTest extends BaseTest {

    // named distinctly from any inherited mock, and constructed into the class under test
    // manually (not via @InjectMocks): several other ConnectorService<?> mocks of different
    // generic parameterizations exist in BaseServiceTest, and since generics are erased at
    // runtime, @InjectMocks constructor resolution by type would be ambiguous.
    @Mock private ConnectorService<LmsUtils> lmsUtilsConnectorService;

    private LmsUtilsImpl lmsUtilsImpl;

    @BeforeEach
    public void beforeEach() {
        MockitoAnnotations.openMocks(this);
        setup();

        lmsUtilsImpl = new LmsUtilsImpl(lmsUtilsConnectorService);
    }

    @Test
    public void testParseCourseIdDelegatesToResolvedInstance() throws TerracottaConnectorException {
        when(lmsUtilsConnectorService.instance(platformDeployment, LmsUtils.class)).thenReturn(lmsUtils);
        when(lmsUtils.parseCourseId(platformDeployment, "https://example.com/course/1")).thenReturn("1");

        String result = lmsUtilsImpl.parseCourseId(platformDeployment, "https://example.com/course/1");

        assertEquals("1", result);
    }

    @Test
    public void testParseDeploymentIdDelegatesToResolvedInstance() throws TerracottaConnectorException {
        when(lmsUtilsConnectorService.instance(platformDeployment, LmsUtils.class)).thenReturn(lmsUtils);
        when(lmsUtils.parseDeploymentId(platformDeployment, "https://example.com/deployment/2")).thenReturn("2");

        String result = lmsUtilsImpl.parseDeploymentId(platformDeployment, "https://example.com/deployment/2");

        assertEquals("2", result);
    }

    @Test
    public void testParseCourseIdPropagatesConnectorException() throws TerracottaConnectorException {
        when(lmsUtilsConnectorService.instance(platformDeployment, LmsUtils.class)).thenThrow(new TerracottaConnectorException("boom"));

        assertThrows(TerracottaConnectorException.class, () -> lmsUtilsImpl.parseCourseId(platformDeployment, "url"));
    }

    @Test
    public void testParseDeploymentIdPropagatesConnectorException() throws TerracottaConnectorException {
        when(lmsUtilsConnectorService.instance(platformDeployment, LmsUtils.class)).thenThrow(new TerracottaConnectorException("boom"));

        assertThrows(TerracottaConnectorException.class, () -> lmsUtilsImpl.parseDeploymentId(platformDeployment, "url"));
    }

    @Test
    public void testSanitizeReplacesAllIllegalCharactersWithUnderscore() {
        String input = "a/b\\c\"d*e<f>g+h=i|j,k%l:m?n~o#p&q{r}s";

        String result = lmsUtilsImpl.sanitize(input);

        assertEquals("a_b_c_d_e_f_g_h_i_j_k_l_m_n_o_p_q_r_s", result);
    }

    @Test
    public void testSanitizeLeavesOrdinaryCharactersUnchanged() {
        String input = "course-title_123 (Fall 2026)";

        String result = lmsUtilsImpl.sanitize(input);

        assertEquals(input, result);
    }

    @Test
    public void testSanitizeNullInputReturnsNull() {
        assertNull(lmsUtilsImpl.sanitize(null));
    }

    @Test
    public void testSanitizeEmptyInputReturnsEmpty() {
        assertEquals("", lmsUtilsImpl.sanitize(""));
    }

}
