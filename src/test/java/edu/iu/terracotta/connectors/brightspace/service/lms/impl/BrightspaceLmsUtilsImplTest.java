package edu.iu.terracotta.connectors.brightspace.service.lms.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

import edu.iu.terracotta.base.BaseTest;

public class BrightspaceLmsUtilsImplTest extends BaseTest {

    // Named distinctly from the inherited `brightspaceLmsUtils` mock field in BaseServiceTest
    // (same type, BrightspaceLmsUtilsImpl) to avoid field hiding/shadowing.
    @InjectMocks private BrightspaceLmsUtilsImpl brightspaceLmsUtilsService;

    @BeforeEach
    public void beforeEach() {
        MockitoAnnotations.openMocks(this);

        setup();
    }

    @Test
    public void testParseCourseId() {
        String ret = brightspaceLmsUtilsService.parseCourseId(platformDeployment, "https://brightspace.example.edu/d2l/api/lp/1.0/orgunit/12345/memberships");

        assertEquals("12345", ret);
    }

    @Test
    public void testParseCourseIdNoMatch() {
        String ret = brightspaceLmsUtilsService.parseCourseId(platformDeployment, "https://brightspace.example.edu/d2l/api/lp/1.0/other/12345/path");

        assertNull(ret);
    }

    @Test
    public void testParseCourseIdNullUrl() {
        String ret = brightspaceLmsUtilsService.parseCourseId(platformDeployment, null);

        assertNull(ret);
    }

    @Test
    public void testParseDeploymentId() {
        String ret = brightspaceLmsUtilsService.parseDeploymentId(platformDeployment, "https://brightspace.example.edu/d2l/api/lp/1.0/deployment/6789/orgunit/12345");

        assertEquals("6789", ret);
    }

    @Test
    public void testParseDeploymentIdNoMatch() {
        String ret = brightspaceLmsUtilsService.parseDeploymentId(platformDeployment, "https://brightspace.example.edu/d2l/api/lp/1.0/other/6789/path");

        assertNull(ret);
    }

    @Test
    public void testParseDeploymentIdNullUrl() {
        String ret = brightspaceLmsUtilsService.parseDeploymentId(platformDeployment, null);

        assertNull(ret);
    }

    @Test
    public void testSanitizeReplacesAllSpecialCharacters() {
        String ret = brightspaceLmsUtilsService.sanitize("a/b\\c\"d*e<f>g+h=i|j,k%l:m?n~o#p&q{r}s");

        assertEquals("a_b_c_d_e_f_g_h_i_j_k_l_m_n_o_p_q_r_s", ret);
    }

    @Test
    public void testSanitizeNoSpecialCharacters() {
        String ret = brightspaceLmsUtilsService.sanitize("plainstring");

        assertEquals("plainstring", ret);
    }

    @Test
    public void testSanitizeEmptyString() {
        String ret = brightspaceLmsUtilsService.sanitize("");

        assertEquals("", ret);
    }

    @Test
    public void testSanitizeNull() {
        String ret = brightspaceLmsUtilsService.sanitize(null);

        assertNull(ret);
    }

}
