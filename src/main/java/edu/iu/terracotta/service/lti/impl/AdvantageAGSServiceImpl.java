package edu.iu.terracotta.service.lti.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.iu.terracotta.exceptions.ConnectionException;
import edu.iu.terracotta.exceptions.helper.ExceptionMessageGenerator;
import edu.iu.terracotta.model.LtiContextEntity;
import edu.iu.terracotta.model.PlatformDeployment;
import edu.iu.terracotta.model.ags.LineItem;
import edu.iu.terracotta.model.ags.LineItems;
import edu.iu.terracotta.model.ags.Result;
import edu.iu.terracotta.model.ags.Results;
import edu.iu.terracotta.model.ags.Score;
import edu.iu.terracotta.model.oauth2.LTIToken;
import edu.iu.terracotta.service.lti.AdvantageAGSService;
import edu.iu.terracotta.service.lti.AdvantageConnectorHelper;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
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
@Service
@SuppressWarnings({"rawtypes", "PMD.GuardLogStatement"})
public class AdvantageAGSServiceImpl implements AdvantageAGSService {

    @Autowired private AdvantageConnectorHelper advantageConnectorHelper;
    @Autowired private ExceptionMessageGenerator exceptionMessageGenerator;

    //Asking for a token with the right scope.
    @Override
    public LTIToken getToken(String type, PlatformDeployment platformDeployment) throws ConnectionException {
        String scope = "https://purl.imsglobal.org/spec/lti-ags/scope/lineitem";

        if ("results".equals(type)) {
            scope = "https://purl.imsglobal.org/spec/lti-ags/scope/result.readonly";
        }

        if ("scores".equals(type)) {
            scope = "https://purl.imsglobal.org/spec/lti-ags/scope/score";
        }

        return advantageConnectorHelper.getToken(platformDeployment, scope);
    }

    @Override
    public LineItems getLineItems(LTIToken ltiToken, LtiContextEntity context) throws ConnectionException {
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

            LineItems lineItems = new LineItems();
            lineItems.getLineItemList().addAll(lineItemsList);

            return lineItems;
        } catch (Exception e) {
            StringBuilder exceptionMsg = new StringBuilder();
            exceptionMsg.append("Can't get the AGS");
            log.error(exceptionMsg.toString(), e);
            throw new ConnectionException(exceptionMessageGenerator.exceptionMessage(exceptionMsg.toString(), e));
        }
    }

    @Override
    public boolean deleteLineItem(LTIToken ltiToken, LtiContextEntity context, String id) throws ConnectionException {
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

            String exceptionMsg = "Can't delete the lineitem with id: " + id;
            log.error(exceptionMsg);
            throw new ConnectionException(exceptionMsg);
        } catch (Exception e) {
            StringBuilder exceptionMsg = new StringBuilder();
            exceptionMsg.append("Can't delete the lineitem with id").append(id);
            log.error(exceptionMsg.toString(), e);
            throw new ConnectionException(exceptionMessageGenerator.exceptionMessage(exceptionMsg.toString(), e));
        }
    }

    @Override
    public LineItem putLineItem(LTIToken ltiToken, LtiContextEntity context, LineItem lineItem) throws ConnectionException {
        try {
            ResponseEntity<LineItem> lineItemsGetResponse = advantageConnectorHelper.createRestTemplate().exchange(
                context.getLineitems() + "/" + lineItem.getId(),
                HttpMethod.PUT,
                advantageConnectorHelper.createTokenizedRequestEntity(ltiToken, lineItem),
                LineItem.class
            );

            if (!lineItemsGetResponse.getStatusCode().is2xxSuccessful()) {
                String exceptionMsg = "Can't put the lineitem " + lineItem.getId();
                log.error(exceptionMsg);
                throw new ConnectionException(exceptionMsg);
            }

            return lineItemsGetResponse.getBody();
        } catch (Exception e) {
            StringBuilder exceptionMsg = new StringBuilder();
            exceptionMsg.append("Can't get put lineitem ").append(lineItem.getId());
            log.error(exceptionMsg.toString(), e);
            throw new ConnectionException(exceptionMessageGenerator.exceptionMessage(exceptionMsg.toString(), e));
        }
    }

    @Override
    public LineItem getLineItem(LTIToken ltiToken, LtiContextEntity context, String id) throws ConnectionException {
        try {
            final String getLineItem = context.getLineitems() + "/" + id;
            ResponseEntity<LineItem> lineItemsGetResponse = advantageConnectorHelper.createRestTemplate().exchange(
                getLineItem,
                HttpMethod.GET,
                advantageConnectorHelper.createTokenizedRequestEntity(ltiToken),
                LineItem.class
            );

            if (!lineItemsGetResponse.getStatusCode().is2xxSuccessful()) {
                String exceptionMsg = "Can't get the lineitem " + id;
                log.error(exceptionMsg);
                throw new ConnectionException(exceptionMsg);
            }

            return lineItemsGetResponse.getBody();
        } catch (Exception e) {
            StringBuilder exceptionMsg = new StringBuilder();
            exceptionMsg.append("Can't get the lineitem ").append(id);
            log.error(exceptionMsg.toString(), e);
            throw new ConnectionException(exceptionMessageGenerator.exceptionMessage(exceptionMsg.toString(), e));
        }
    }

    @Override
    public LineItems postLineItems(LTIToken ltiToken, LtiContextEntity context, LineItems lineItems) throws ConnectionException {
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

            LineItems resultLineItems = new LineItems();
            resultLineItems.getLineItemList().addAll(lineItemsList);

            return resultLineItems;
        } catch (Exception e) {
            StringBuilder exceptionMsg = new StringBuilder();
            exceptionMsg.append("Can't post lineitems");
            log.error(exceptionMsg.toString(), e);
            throw new ConnectionException(exceptionMessageGenerator.exceptionMessage(exceptionMsg.toString(), e));
        }
    }

    @Override
    public Results getResults(LTIToken ltiTokenResults, LtiContextEntity context, String lineItemId) throws ConnectionException {
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
    public void postScore(LTIToken lTITokenScores, LTIToken lTITokenResults, LtiContextEntity context, String lineItemId, Score score) throws ConnectionException {
        try {
            ResponseEntity<Void> scoreGetResponse = advantageConnectorHelper.createRestTemplate().exchange(
                lineItemId + "/scores",
                HttpMethod.POST,
                advantageConnectorHelper.createTokenizedRequestEntity(lTITokenScores, new ObjectMapper().writeValueAsString(score)),
                Void.class
            );

            if (scoreGetResponse.getStatusCode().is2xxSuccessful()) {
                return;
            }

            String exceptionMsg = "Can't post scores";
            log.error(exceptionMsg);
            throw new ConnectionException(exceptionMsg);
        } catch (Exception e) {
            StringBuilder exceptionMsg = new StringBuilder();
            exceptionMsg.append("Can't post scores");
            log.error(exceptionMsg.toString(), e);
            throw new ConnectionException(exceptionMessageGenerator.exceptionMessage(exceptionMsg.toString(), e));
        }
    }

}
