package edu.iu.terracotta.connectors.oneedtech.service.lti.advantage.impl;

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

import edu.iu.terracotta.connectors.generic.annotation.TerracottaConnector;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.LtiContextEntity;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.PlatformDeployment;
import edu.iu.terracotta.connectors.generic.dao.model.enums.LmsConnector;
import edu.iu.terracotta.connectors.generic.dao.model.lti.LtiToken;
import edu.iu.terracotta.connectors.generic.dao.model.lti.ags.LineItem;
import edu.iu.terracotta.connectors.generic.dao.model.lti.ags.LineItems;
import edu.iu.terracotta.connectors.generic.dao.model.lti.ags.Results;
import edu.iu.terracotta.connectors.generic.dao.model.lti.ags.Score;
import edu.iu.terracotta.connectors.generic.dao.model.lti.enums.LtiAgsScope;
import edu.iu.terracotta.connectors.generic.exceptions.ConnectionException;
import edu.iu.terracotta.connectors.generic.exceptions.TerracottaConnectorException;
import edu.iu.terracotta.connectors.generic.exceptions.helper.ExceptionMessageGenerator;
import edu.iu.terracotta.connectors.generic.service.lti.advantage.AdvantageAgsService;
import edu.iu.terracotta.connectors.generic.service.lti.advantage.AdvantageConnectorHelper;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.json.JsonMapper;

@Slf4j
@Service
@SuppressWarnings({"rawtypes", "PMD.GuardLogStatement"})
@TerracottaConnector(LmsConnector.ONE_ED_TECH)
public class OneEdTechAdvantageAgsServiceImpl implements AdvantageAgsService {

    @Autowired private AdvantageConnectorHelper advantageConnectorHelper;
    @Autowired private ExceptionMessageGenerator exceptionMessageGenerator;

    @Override
    public LtiToken getToken(LtiAgsScope type, PlatformDeployment platformDeployment) throws ConnectionException {
        String scope;

        switch (type) {
            case LINEITEM -> scope = LtiAgsScope.AGS_LINEITEM.key();
            case RESULTS -> scope = LtiAgsScope.AGS_RESULT_READONLY.key();
            case SCORES -> scope = LtiAgsScope.AGS_SCORE.key();
            default -> scope = LtiAgsScope.AGS_LINEITEM.key();
        }

        return advantageConnectorHelper.getToken(platformDeployment, scope);
    }

    @Override
    public LineItem postLineItem(LtiToken ltiToken, LtiContextEntity context, LineItem lineItem) throws ConnectionException {
        try {
            ResponseEntity<LineItem> response = advantageConnectorHelper.createRestTemplate().exchange(
                context.getLineitems(),
                HttpMethod.POST,
                advantageConnectorHelper.createTokenizedRequestEntityWithAcceptAndContentType(
                    ltiToken,
                    lineItem,
                    LtiAgsScope.LIS_LINEITEM_JSON.key(),
                    LtiAgsScope.LIS_LINEITEM_JSON.key()
                ),
                LineItem.class
            );

            if (!response.getStatusCode().is2xxSuccessful()) {
                String exceptionMsg = String.format("Can't post the lineitem [%s]", lineItem.getId());
                log.error(exceptionMsg);
                throw new ConnectionException(exceptionMsg);
            }

            log.info(
                "POST lineItem to: [{}] response: [{}]",
                context.getLineitems(),
                JsonMapper.builder()
                    .build()
                    .writeValueAsString(response.getBody())
            );

            return response.getBody();
        } catch (Exception e) {
            String exceptionMsg = String.format("Can't post lineitem for assignment ID: [%s]", lineItem.getId());
            log.error(exceptionMsg, e);
            throw new ConnectionException(exceptionMessageGenerator.exceptionMessage(exceptionMsg, e));
        }
    }

    @Override
    public LineItems getLineItems(LtiToken ltiToken, LtiContextEntity context) throws ConnectionException {
        try {
            RestTemplate restTemplate = advantageConnectorHelper.createRestTemplate();
            HttpEntity request = advantageConnectorHelper.createTokenizedRequestEntityWithAcceptAndContentType(
                ltiToken,
                LtiAgsScope.LIS_LINEITEM_CONTAINER_JSON.key(),
                LtiAgsScope.LIS_LINEITEM_CONTAINER_JSON.key()
            );
            ResponseEntity<LineItem[]> response = restTemplate.exchange(context.getLineitems(), HttpMethod.GET, request, LineItem[].class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                String exceptionMsg = "Can't get the AGS";
                log.error(exceptionMsg);
                throw new ConnectionException(exceptionMsg);
            }

            List<LineItem> lineItemsList = new ArrayList<>(Arrays.asList(Objects.requireNonNull(response.getBody())));
            String nextPage = advantageConnectorHelper.nextPage(response.getHeaders());

            while (nextPage != null) {
                ResponseEntity<LineItem[]> responseForNextPage = restTemplate.exchange(nextPage, HttpMethod.GET, request, LineItem[].class);
                LineItem[] nextLineItemsList = responseForNextPage.getBody();
                lineItemsList.addAll(Arrays.asList(nextLineItemsList));
                nextPage = advantageConnectorHelper.nextPage(responseForNextPage.getHeaders());
            }

            // 1EdTech doesn't return the resourceLinkId in the response, so we set from the id field
            lineItemsList.forEach(lineItem -> lineItem.setResourceLinkId(lineItem.getId()));

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
    public LineItems postLineItems(LtiToken ltiToken, LtiContextEntity context, LineItems lineItems) throws ConnectionException {
        throw new UnsupportedOperationException("Unimplemented method 'postLineItems'");
    }

    @Override
    public Results getResults(LtiToken ltiTokenResults, LtiContextEntity context, String lineItemId) throws ConnectionException {
        throw new UnsupportedOperationException("Unimplemented method 'getResults'");
    }

    @Override
    public void postScore(LtiToken ltiTokenScores, LtiToken ltiTokenResults, LtiContextEntity context, String lineItemUrl, Score score) throws ConnectionException, TerracottaConnectorException {
        try {
            ResponseEntity<Void> scoreGetResponse = advantageConnectorHelper.createRestTemplate().exchange(
                String.format("%s/scores", lineItemUrl),
                HttpMethod.POST,
                advantageConnectorHelper.createTokenizedRequestEntity(
                    ltiTokenScores,
                    JsonMapper.builder()
                        .build()
                        .writeValueAsString(score)
                ),
                Void.class
            );

            if (scoreGetResponse.getStatusCode().is2xxSuccessful()) {
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
