package edu.iu.terracotta.connectors.canvas.service.api.impl;

import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

import edu.iu.terracotta.base.BaseTest;
import edu.iu.terracotta.connectors.canvas.service.lti.advantage.impl.CanvasAdvantageAgsServiceImpl;
import edu.iu.terracotta.connectors.generic.exceptions.ConnectionException;
import edu.iu.terracotta.connectors.generic.exceptions.TerracottaConnectorException;

public class CanvasAdvantageAgsServiceImplTest extends BaseTest {

    @InjectMocks private CanvasAdvantageAgsServiceImpl canvasAdvantageAgsService;

    @BeforeEach
    public void beforeEach() {
        MockitoAnnotations.openMocks(this);

        setup();
    }

    @Test
    public void testDeleteLineItem() throws ConnectionException, TerracottaConnectorException {
        boolean ret = canvasAdvantageAgsService.deleteLineItem(ltiToken, ltiContextEntity, "lineItemId");

        assertTrue(ret);
    }

}
