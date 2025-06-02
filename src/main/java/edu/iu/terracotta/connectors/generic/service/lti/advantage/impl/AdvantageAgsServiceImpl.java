package edu.iu.terracotta.connectors.generic.service.lti.advantage.impl;

import edu.iu.terracotta.connectors.generic.dao.entity.lti.LtiContextEntity;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.PlatformDeployment;
import edu.iu.terracotta.connectors.generic.dao.model.lti.LtiToken;
import edu.iu.terracotta.connectors.generic.dao.model.lti.ags.LineItem;
import edu.iu.terracotta.connectors.generic.dao.model.lti.ags.LineItems;
import edu.iu.terracotta.connectors.generic.dao.model.lti.ags.Result;
import edu.iu.terracotta.connectors.generic.dao.model.lti.ags.Results;
import edu.iu.terracotta.connectors.generic.dao.model.lti.ags.Score;
import edu.iu.terracotta.connectors.generic.exceptions.ConnectionException;
import edu.iu.terracotta.connectors.generic.exceptions.TerracottaConnectorException;
import edu.iu.terracotta.connectors.generic.exceptions.helper.ExceptionMessageGenerator;
import edu.iu.terracotta.connectors.generic.service.connector.ConnectorService;
import edu.iu.terracotta.connectors.generic.service.lti.advantage.AdvantageAgsService;
import edu.iu.terracotta.connectors.generic.service.lti.advantage.AdvantageConnectorHelper;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * This manages all the Membership call for the LTIRequest (and for LTI in general)
 * Necessary to get appropriate TX handling and service management
 */
@Slf4j
@Primary
@Service
@SuppressWarnings({"rawtypes", "PMD.GuardLogStatement", "PMD.UnusedPrivateMethod"})
public class AdvantageAgsServiceImpl implements AdvantageAgsService {

    @Autowired private ConnectorService<AdvantageAgsService> connectorService;
    @Autowired private AdvantageConnectorHelper advantageConnectorHelper;
    @Autowired private ExceptionMessageGenerator exceptionMessageGenerator;

    private AdvantageAgsService instance(LtiContextEntity ltiContextEntity) throws TerracottaConnectorException {
        return instance(ltiContextEntity.getToolDeployment().getPlatformDeployment());
    }

    private AdvantageAgsService instance(PlatformDeployment platformDeployment) throws TerracottaConnectorException {
        return connectorService.instance(platformDeployment, AdvantageAgsService.class);
    }

    //Asking for a token with the right scope.
    @Override
    public LtiToken getToken(String type, PlatformDeployment platformDeployment) throws ConnectionException {
        String scope = "https://purl.imsglobal.org/spec/lti-ags/scope/lineitem";

        if (Strings.CS.equals(type, "results")) {
            scope = "https://purl.imsglobal.org/spec/lti-ags/scope/result.readonly";
        }

        if (Strings.CS.equals(type, "scores")) {
            scope = "https://purl.imsglobal.org/spec/lti-ags/scope/score";
        }

        return advantageConnectorHelper.getToken(platformDeployment, scope);
    }

    @Override
    public LineItems getLineItems(LtiToken ltiToken, LtiContextEntity ltiContextEntity) throws ConnectionException, TerracottaConnectorException {
        return instance(ltiContextEntity)
            .getLineItems(ltiToken, ltiContextEntity);
    }

    @Override
    public boolean deleteLineItem(LtiToken ltiToken, LtiContextEntity context, String id) throws ConnectionException {
        try {
            ResponseEntity<String> lineItemsGetResponse = advantageConnectorHelper.createRestTemplate().exchange(
                context.getLineitems() + "/" + id,
                HttpMethod.DELETE,
                advantageConnectorHelper.createTokenizedRequestEntity(ltiToken),
                String.class
            );

            if (lineItemsGetResponse.getStatusCode().is2xxSuccessful()) {
                return true;
            }

            String exceptionMsg = String.format("Can't delete the lineitem with id: %s", id);
            log.error(exceptionMsg);
            throw new ConnectionException(exceptionMsg);
        } catch (Exception e) {
            StringBuilder exceptionMsg = new StringBuilder()
                .append("Can't delete the lineitem with id ")
                .append(id);
            log.error(exceptionMsg.toString(), e);
            throw new ConnectionException(exceptionMessageGenerator.exceptionMessage(exceptionMsg.toString(), e));
        }
    }

    @Override
    public LineItem postLineItem(LtiToken ltiToken, LtiContextEntity context, LineItem lineItem) throws ConnectionException {
        try {
            ResponseEntity<LineItem> response = advantageConnectorHelper.createRestTemplate().exchange(
                context.getLineitems(),
                HttpMethod.POST,
                advantageConnectorHelper.createTokenizedRequestEntityWithAcceptAndContentType(ltiToken, lineItem, "application/vnd.ims.lis.v2.lineitem+json", "application/vnd.ims.lis.v2.lineitem+json"),
                LineItem.class
            );

            if (!response.getStatusCode().is2xxSuccessful()) {
                String exceptionMsg = String.format("Can't post the lineitem [%s]", lineItem.getId());
                log.error(exceptionMsg);
                throw new ConnectionException(exceptionMsg);
            }

            return response.getBody();
        } catch (Exception e) {
            StringBuilder exceptionMsg = new StringBuilder()
                .append("Can't get post lineitem ")
                .append(lineItem.getId());
            log.error(exceptionMsg.toString(), e);
            throw new ConnectionException(exceptionMessageGenerator.exceptionMessage(exceptionMsg.toString(), e));
        }
    }

    @Override
    public LineItem putLineItem(LtiToken ltiToken, LtiContextEntity context, LineItem lineItem) throws ConnectionException {
        try {
            ResponseEntity<LineItem> lineItemsGetResponse = advantageConnectorHelper.createRestTemplate().exchange(
                context.getLineitems() + "/" + lineItem.getId(),
                HttpMethod.PUT,
                advantageConnectorHelper.createTokenizedRequestEntity(ltiToken, lineItem),
                LineItem.class
            );

            if (!lineItemsGetResponse.getStatusCode().is2xxSuccessful()) {
                String exceptionMsg = String.format("Can't put the lineitem %s", lineItem.getId());
                log.error(exceptionMsg);
                throw new ConnectionException(exceptionMsg);
            }

            return lineItemsGetResponse.getBody();
        } catch (Exception e) {
            StringBuilder exceptionMsg = new StringBuilder()
                .append("Can't get put lineitem ")
                .append(lineItem.getId());
            log.error(exceptionMsg.toString(), e);
            throw new ConnectionException(exceptionMessageGenerator.exceptionMessage(exceptionMsg.toString(), e));
        }
    }

    @Override
    public LineItem getLineItem(LtiToken ltiToken, LtiContextEntity context, String id) throws ConnectionException {
        try {
            final String getLineItem = context.getLineitems() + "/" + id;
            ResponseEntity<LineItem> lineItemsGetResponse = advantageConnectorHelper.createRestTemplate().exchange(
                getLineItem,
                HttpMethod.GET,
                advantageConnectorHelper.createTokenizedRequestEntity(ltiToken),
                LineItem.class
            );

            if (!lineItemsGetResponse.getStatusCode().is2xxSuccessful()) {
                String exceptionMsg = String.format("Can't get the lineitem %s", id);
                log.error(exceptionMsg);
                throw new ConnectionException(exceptionMsg);
            }

            return lineItemsGetResponse.getBody();
        } catch (Exception e) {
            StringBuilder exceptionMsg = new StringBuilder()
                .append("Can't get the lineitem ")
                .append(id);
            log.error(exceptionMsg.toString(), e);
            throw new ConnectionException(exceptionMessageGenerator.exceptionMessage(exceptionMsg.toString(), e));
        }
    }

    @Override
    public LineItems postLineItems(LtiToken ltiToken, LtiContextEntity context, LineItems lineItems) throws ConnectionException {
        try {
            RestTemplate restTemplate = advantageConnectorHelper.createRestTemplate();
            HttpEntity<LineItems> request = advantageConnectorHelper.createTokenizedRequestEntity(ltiToken, lineItems);
            ResponseEntity<LineItem[]> lineItemsGetResponse = restTemplate.exchange(context.getLineitems(), HttpMethod.POST, request, LineItem[].class);

            if (!lineItemsGetResponse.getStatusCode().is2xxSuccessful()) {
                String exceptionMsg = "Can't post lineitems";
                log.error(exceptionMsg);
                throw new ConnectionException(exceptionMsg);
            }

            List<LineItem> lineItemsList = new ArrayList<>(Arrays.asList(Objects.requireNonNull(lineItemsGetResponse.getBody())));
            String nextPage = advantageConnectorHelper.nextPage(lineItemsGetResponse.getHeaders());

            while (nextPage != null) {
                ResponseEntity<LineItems> responseForNextPage = restTemplate.exchange(nextPage, HttpMethod.GET, request, LineItems.class);
                LineItems nextLineItemsList = responseForNextPage.getBody();
                List<LineItem> nextLineItems = Objects.requireNonNull(nextLineItemsList).getLineItemList();
                lineItemsList.addAll(nextLineItems);
                nextPage = advantageConnectorHelper.nextPage(responseForNextPage.getHeaders());
            }

            return LineItems.builder()
                .lineItemList(lineItemsList)
                .build();
        } catch (Exception e) {
            String exceptionMsg = "Can't post lineitems";
            log.error(exceptionMsg, e);
            throw new ConnectionException(exceptionMessageGenerator.exceptionMessage(exceptionMsg, e));
        }
    }

    @Override
    public Results getResults(LtiToken ltiTokenResults, LtiContextEntity context, String lineItemId) throws ConnectionException {
        try {
            RestTemplate restTemplate = advantageConnectorHelper.createRestTemplate();
            HttpEntity request = advantageConnectorHelper.createTokenizedRequestEntity(ltiTokenResults);
            ResponseEntity<Result[]> resultsGetResponse = restTemplate.exchange(lineItemId + "/results", HttpMethod.GET, request, Result[].class);

            if (!resultsGetResponse.getStatusCode().is2xxSuccessful()) {
                String exceptionMsg = "Can't get the AGS";
                log.error(exceptionMsg);
                throw new ConnectionException(exceptionMsg);
            }

            List<Result> resultList = new ArrayList<>(Arrays.asList(Objects.requireNonNull(resultsGetResponse.getBody())));
            String nextPage = advantageConnectorHelper.nextPage(resultsGetResponse.getHeaders());

            while (nextPage != null) {
                ResponseEntity<Result[]> responseForNextPage = restTemplate.exchange(nextPage, HttpMethod.GET, request, Result[].class);
                List<Result> nextResults = new ArrayList<>(Arrays.asList(Objects.requireNonNull(responseForNextPage.getBody())));
                resultList.addAll(nextResults);
                nextPage = advantageConnectorHelper.nextPage(responseForNextPage.getHeaders());
            }

            Results results = new Results();
            results.getResultList().addAll(resultList);

            return results;
        } catch (Exception e) {
            StringBuilder exceptionMsg = new StringBuilder();
            exceptionMsg.append("Can't get the AGS");
            log.error(exceptionMsg.toString(), e);
            throw new ConnectionException(exceptionMessageGenerator.exceptionMessage(exceptionMsg.toString(), e));
        }
    }

    @Override
    public void postScore(LtiToken ltiTokenScores, LtiToken ltiTokenResults, LtiContextEntity ltiContextEntity, String lineItemId, Score score) throws ConnectionException, TerracottaConnectorException {
        instance(ltiContextEntity)
            .postScore(ltiTokenScores, ltiTokenResults, ltiContextEntity, lineItemId, score);
    }

}
