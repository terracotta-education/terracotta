package edu.iu.terracotta.connectors.canvas.service.lti.advantage.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

import edu.iu.terracotta.connectors.canvas.dao.model.lti.ags.CanvasScore;
import edu.iu.terracotta.connectors.generic.annotation.TerracottaConnector;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.LtiContextEntity;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.PlatformDeployment;
import edu.iu.terracotta.connectors.generic.dao.model.enums.LmsConnector;
import edu.iu.terracotta.connectors.generic.dao.model.lti.LtiToken;
import edu.iu.terracotta.connectors.generic.dao.model.lti.ags.LineItem;
import edu.iu.terracotta.connectors.generic.dao.model.lti.ags.LineItems;
import edu.iu.terracotta.connectors.generic.dao.model.lti.ags.Results;
import edu.iu.terracotta.connectors.generic.dao.model.lti.ags.Score;
import edu.iu.terracotta.connectors.generic.exceptions.ConnectionException;
import edu.iu.terracotta.connectors.generic.exceptions.helper.ExceptionMessageGenerator;
import edu.iu.terracotta.connectors.generic.service.lti.advantage.AdvantageAgsService;
import edu.iu.terracotta.connectors.generic.service.lti.advantage.AdvantageConnectorHelper;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@TerracottaConnector(LmsConnector.CANVAS)
@SuppressWarnings({"rawtypes", "PMD.GuardLogStatement"})
public class CanvasAdvantageAgsServiceImpl implements AdvantageAgsService {

    @Autowired private AdvantageConnectorHelper advantageConnectorHelper;
    @Autowired private ExceptionMessageGenerator exceptionMessageGenerator;

    @Override
    public LtiToken getToken(String scope, PlatformDeployment platformDeployment) throws ConnectionException {
        throw new UnsupportedOperationException("Unimplemented method 'getToken'");
    }

    @Override
    public LineItems getLineItems(LtiToken ltiToken, LtiContextEntity context) throws ConnectionException {
        try {
            RestTemplate restTemplate = advantageConnectorHelper.createRestTemplate();
            HttpEntity request = advantageConnectorHelper.createTokenizedRequestEntity(ltiToken);
            ResponseEntity<LineItem[]> lineItemsGetResponse = restTemplate.exchange(context.getLineitems(), HttpMethod.GET, request, LineItem[].class);

            if (!lineItemsGetResponse.getStatusCode().is2xxSuccessful()) {
                String exceptionMsg = "Can't get the AGS";
                log.error(exceptionMsg);
                throw new ConnectionException(exceptionMsg);
            }

            List<LineItem> lineItemsList = new ArrayList<>(Arrays.asList(Objects.requireNonNull(lineItemsGetResponse.getBody())));
            String nextPage = advantageConnectorHelper.nextPage(lineItemsGetResponse.getHeaders());

            while (nextPage != null) {
                ResponseEntity<LineItem[]> responseForNextPage = restTemplate.exchange(nextPage, HttpMethod.GET, request, LineItem[].class);
                LineItem[] nextLineItemsList = responseForNextPage.getBody();
                lineItemsList.addAll(Arrays.asList(nextLineItemsList));
                nextPage = advantageConnectorHelper.nextPage(responseForNextPage.getHeaders());
            }

            return LineItems.builder()
                .lineItemList(lineItemsList)
                .build();
        } catch (Exception e) {
            String exceptionMsg = "Can't get the AGS";
            log.error(exceptionMsg, e);
            throw new ConnectionException(exceptionMessageGenerator.exceptionMessage(exceptionMsg, e));
        }
    }

    @Override
    public boolean deleteLineItem(LtiToken ltiToken, LtiContextEntity context, String id) throws ConnectionException {
        throw new UnsupportedOperationException("Unimplemented method 'deleteLineItem'");
    }

    @Override
    public LineItem putLineItem(LtiToken ltiToken, LtiContextEntity context, LineItem lineItem) throws ConnectionException {
        throw new UnsupportedOperationException("Unimplemented method 'putLineItem'");
    }

    @Override
    public LineItem getLineItem(LtiToken ltiToken, LtiContextEntity context, String id) throws ConnectionException {
        throw new UnsupportedOperationException("Unimplemented method 'getLineItem'");
    }

    @Override
    public LineItem postLineItem(LtiToken ltiToken, LtiContextEntity context, LineItem lineItem) throws ConnectionException {
        throw new UnsupportedOperationException("Unimplemented method 'postLineItem'");
    }

    @Override
    public LineItems postLineItems(LtiToken ltiToken, LtiContextEntity context, LineItems lineItems) throws ConnectionException {
        throw new UnsupportedOperationException("Unimplemented method 'postLineItems'");
    }

    @Override
    public Results getResults(LtiToken ltiTokenResults, LtiContextEntity context, String lineItemId) throws ConnectionException {
        throw new UnsupportedOperationException("Unimplemented method 'getResults'");
    }

    @Override
    public void postScore(LtiToken ltiTokenScores, LtiToken ltiTokenResults, LtiContextEntity context, String lineItemId, Score score) throws ConnectionException {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.addMixIn(Score.class, CanvasScore.class); // mixin to add Canvas-specific submission key

            ResponseEntity<Void> response = advantageConnectorHelper.createRestTemplate().exchange(
                lineItemId + "/scores",
                HttpMethod.POST,
                advantageConnectorHelper.createTokenizedRequestEntity(ltiTokenScores, objectMapper.writeValueAsString(score)),
                Void.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                return;
            }

            String exceptionMsg = "Can't post scores";
            log.error(exceptionMsg);
            throw new ConnectionException(exceptionMsg);
        } catch (Exception e) {
            String exceptionMsg = "Can't post scores";
            log.error(exceptionMsg.toString(), e);
            throw new ConnectionException(exceptionMessageGenerator.exceptionMessage(exceptionMsg.toString(), e));
        }
    }

}
