package edu.iu.terracotta.controller.lti;

import edu.iu.terracotta.connectors.generic.dao.entity.lti.LtiContextEntity;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.ToolDeployment;
import edu.iu.terracotta.connectors.generic.dao.model.lti.LtiToken;
import edu.iu.terracotta.connectors.generic.dao.model.lti.ags.LineItem;
import edu.iu.terracotta.connectors.generic.dao.model.lti.ags.LineItems;
import edu.iu.terracotta.connectors.generic.dao.model.lti.enums.LtiAgsScope;
import edu.iu.terracotta.connectors.generic.dao.repository.lti.LtiContextRepository;
import edu.iu.terracotta.connectors.generic.dao.repository.lti.ToolDeploymentRepository;
import edu.iu.terracotta.connectors.generic.exceptions.ConnectionException;
import edu.iu.terracotta.connectors.generic.exceptions.TerracottaConnectorException;
import edu.iu.terracotta.connectors.generic.service.lti.advantage.AdvantageAgsService;
import edu.iu.terracotta.utils.LtiStrings;
import edu.iu.terracotta.utils.TextConstants;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.security.Principal;
import java.util.Collections;
import java.util.Optional;

/**
 * This LTI 3 redirect controller will retrieve the LTI3 requests and redirect them to the right page.
 * Everything that arrives here is filtered first by the LTI3OAuthProviderProcessingFilter
 */
@Slf4j
@Controller
@Scope("session")
@RequestMapping("/ags")
@SuppressWarnings({"SameReturnValue", "PMD.GuardLogStatement"})
public class AgsController {

    public static final String LTIADVAGSMAIN = "ltiAdvAgsMain";

    @Autowired private LtiContextRepository ltiContextRepository;
    @Autowired private ToolDeploymentRepository toolDeploymentRepository;
    @Autowired private AdvantageAgsService advantageAGSServiceService;

    @GetMapping("/")
    public String agsGetLineItems(HttpServletRequest req, Principal principal, Model model) throws ConnectionException, TerracottaConnectorException {
        //To keep this endpoint secured, we will only allow access to the course/platform stored in the session.
        //LTI Advantage services doesn't need a session to access to the membership, but we implemented this control here
        // to avoid access to all the courses and platforms.
        HttpSession session = req.getSession();

        if (session.getAttribute(LtiStrings.LTI_SESSION_TOOL_DEPLOYMENT_ID) == null) {
            model.addAttribute(TextConstants.NO_SESSION_VALUES, true);
        }

        model.addAttribute(TextConstants.NO_SESSION_VALUES, false);
        Long toolDeploymentId = (Long) session.getAttribute(LtiStrings.LTI_SESSION_TOOL_DEPLOYMENT_ID);
        String contextId = (String) session.getAttribute(LtiStrings.LTI_SESSION_CONTEXT_ID);
        //We find the right deployment:
        Optional<ToolDeployment> toolDeployment = toolDeploymentRepository.findById(toolDeploymentId);

        if (toolDeployment.isPresent()) {
            //Get the context in the query
            LtiContextEntity context = ltiContextRepository.findByContextKeyAndToolDeployment(contextId, toolDeployment.get());

            //Call the ags service to get the users on the context
            // 1. Get the token
            LtiToken ltiToken = advantageAGSServiceService.getToken(LtiAgsScope.LINEITEMS, toolDeployment.get().getPlatformDeployment());
            log.info(TextConstants.TOKEN + ltiToken.getAccess_token());
            // 2. Call the service
            LineItems lineItemsResult = advantageAGSServiceService.getLineItems(ltiToken, context);

            // 3. update the model
            model.addAttribute(LtiAgsScope.SINGLE.key(), false);
            model.addAttribute(LtiAgsScope.RESULTS.key(), lineItemsResult.getLineItemList());
        }

        return LTIADVAGSMAIN;
    }


    // Create a new lineitem
    @PostMapping("/")
    public String agsPostLineItem(HttpServletRequest req, Principal principal, Model model, @RequestBody LineItems lineItems) throws ConnectionException {
        //To keep this endpoint secured, we will only allow access to the course/platform stored in the session.
        //LTI Advantage services doesn't need a session to access to the membership, but we implemented this control here
        // to avoid access to all the courses and platforms.
        HttpSession session = req.getSession();

        if (session.getAttribute(LtiStrings.LTI_SESSION_TOOL_DEPLOYMENT_ID) == null) {
            model.addAttribute(TextConstants.NO_SESSION_VALUES, true);
        }

        model.addAttribute(TextConstants.NO_SESSION_VALUES, false);
        Long toolDeploymentId = (Long) session.getAttribute(LtiStrings.LTI_SESSION_TOOL_DEPLOYMENT_ID);
        String contextId = (String) session.getAttribute(LtiStrings.LTI_SESSION_CONTEXT_ID);
        //We find the right deployment:
        Optional<ToolDeployment> toolDeployment = toolDeploymentRepository.findById(toolDeploymentId);

        if (toolDeployment.isPresent()) {
            //Get the context in the query
            LtiContextEntity context = ltiContextRepository.findByContextKeyAndToolDeployment(contextId, toolDeployment.get());

            //Call the ags service to post a lineitem
            // 1. Get the token
            LtiToken ltiToken = advantageAGSServiceService.getToken(LtiAgsScope.LINEITEMS, toolDeployment.get().getPlatformDeployment());
            log.info(TextConstants.TOKEN + ltiToken.getAccess_token());

            // 2. Call the service
            LineItems lineItemsResult = advantageAGSServiceService.postLineItems(ltiToken, context, lineItems);

            // 3. update the model
            model.addAttribute(LtiAgsScope.SINGLE.key(), false);
            model.addAttribute(LtiAgsScope.RESULTS.key(), lineItemsResult.getLineItemList());
        }

        return LTIADVAGSMAIN;
    }

    // Get specific lineitem
    @GetMapping("/{id}")
    public String agsGetLineitem(HttpServletRequest req, Principal principal, Model model, @PathVariable("id") String id) throws ConnectionException {
        //To keep this endpoint secured, we will only allow access to the course/platform stored in the session.
        //LTI Advantage services doesn't need a session to access to the membership, but we implemented this control here
        // to avoid access to all the courses and platforms.
        HttpSession session = req.getSession();

        if (session.getAttribute(LtiStrings.LTI_SESSION_TOOL_DEPLOYMENT_ID) == null) {
            model.addAttribute(TextConstants.NO_SESSION_VALUES, true);
        }

        model.addAttribute(TextConstants.NO_SESSION_VALUES, false);
        Long toolDeploymentId = (Long) session.getAttribute(LtiStrings.LTI_SESSION_TOOL_DEPLOYMENT_ID);
        String contextId = (String) session.getAttribute(LtiStrings.LTI_SESSION_CONTEXT_ID);
        //We find the right deployment:
        Optional<ToolDeployment> toolDeployment = toolDeploymentRepository.findById(toolDeploymentId);

        if (toolDeployment.isPresent()) {
            //Get the context in the query
            LtiContextEntity context = ltiContextRepository.findByContextKeyAndToolDeployment(contextId, toolDeployment.get());

            //Call the ags service to post a lineitem
            // 1. Get the token
            LtiToken ltiToken = advantageAGSServiceService.getToken(LtiAgsScope.LINEITEMS, toolDeployment.get().getPlatformDeployment());
            log.info(TextConstants.TOKEN + ltiToken.getAccess_token());

            // 2. Call the service
            LineItem lineItemsResult = advantageAGSServiceService.getLineItem(ltiToken, context, id);

            // 3. update the model
            model.addAttribute(LtiAgsScope.SINGLE.key(), true);
            model.addAttribute(LtiAgsScope.RESULTS.key(), Collections.singletonList(lineItemsResult));
        }

        return LTIADVAGSMAIN;
    }

    // Put specific lineitem
    @PutMapping("/{id}")
    public String agsPutLineitem(HttpServletRequest req, Principal principal, Model model, @RequestBody LineItem lineItem, @PathVariable("id") String id) throws ConnectionException {
        //To keep this endpoint secured, we will only allow access to the course/platform stored in the session.
        //LTI Advantage services doesn't need a session to access to the membership, but we implemented this control here
        // to avoid access to all the courses and platforms.
        HttpSession session = req.getSession();

        if (session.getAttribute(LtiStrings.LTI_SESSION_TOOL_DEPLOYMENT_ID) == null) {
            model.addAttribute(TextConstants.NO_SESSION_VALUES, true);
        }

            model.addAttribute(TextConstants.NO_SESSION_VALUES, false);
            Long toolDeploymentId = (Long) session.getAttribute(LtiStrings.LTI_SESSION_TOOL_DEPLOYMENT_ID);
            String contextId = (String) session.getAttribute(LtiStrings.LTI_SESSION_CONTEXT_ID);
            //We find the right deployment:
            Optional<ToolDeployment> toolDeployment = toolDeploymentRepository.findById(toolDeploymentId);

            if (toolDeployment.isPresent()) {
                //Get the context in the query
                LtiContextEntity context = ltiContextRepository.findByContextKeyAndToolDeployment(contextId, toolDeployment.get());

                //Call the ags service to post a lineitem
                // 1. Get the token
                LtiToken ltiToken = advantageAGSServiceService.getToken(LtiAgsScope.LINEITEMS, toolDeployment.get().getPlatformDeployment());
                log.info(TextConstants.TOKEN + ltiToken.getAccess_token());

                // 2. Call the service
                lineItem.setId(id);
                LineItem lineItemsResult = advantageAGSServiceService.putLineItem(ltiToken, context, lineItem);

                // 3. update the model
                model.addAttribute(LtiAgsScope.SINGLE.key(), true);
                model.addAttribute(LtiAgsScope.RESULTS.key(), Collections.singletonList(lineItemsResult));
            }

        return LTIADVAGSMAIN;
    }


    // Delete lineitem

    @GetMapping("/delete/{id}")
    public String agsPDeleteLineitem(HttpServletRequest req, Principal principal, Model model, @PathVariable("id") String id) throws ConnectionException, TerracottaConnectorException {
        //To keep this endpoint secured, we will only allow access to the course/platform stored in the session.
        //LTI Advantage services doesn't need a session to access to the membership, but we implemented this control here
        // to avoid access to all the courses and platforms.
        HttpSession session = req.getSession();

        if (session.getAttribute(LtiStrings.LTI_SESSION_TOOL_DEPLOYMENT_ID) == null) {
            model.addAttribute(TextConstants.NO_SESSION_VALUES, true);
        }

        model.addAttribute(TextConstants.NO_SESSION_VALUES, false);
        Long toolDeploymentId = (Long) session.getAttribute(LtiStrings.LTI_SESSION_TOOL_DEPLOYMENT_ID);
        String contextId = (String) session.getAttribute(LtiStrings.LTI_SESSION_CONTEXT_ID);
        //We find the right deployment:
        Optional<ToolDeployment> toolDeployment = toolDeploymentRepository.findById(toolDeploymentId);

        if (toolDeployment.isPresent()) {
            //Get the context in the query
            LtiContextEntity context = ltiContextRepository.findByContextKeyAndToolDeployment(contextId, toolDeployment.get());

            //Call the ags service to post a lineitem
            // 1. Get the token
            LtiToken ltiToken = advantageAGSServiceService.getToken(LtiAgsScope.LINEITEMS, toolDeployment.get().getPlatformDeployment());
            log.info(TextConstants.TOKEN + ltiToken.getAccess_token());

            // 2. Call the service
            Boolean deleteResult = advantageAGSServiceService.deleteLineItem(ltiToken, context, id);
            LineItems lineItemsResult = advantageAGSServiceService.getLineItems(ltiToken, context);

            // 3. update the model
            model.addAttribute(LtiAgsScope.SINGLE.key(), false);
            model.addAttribute(LtiAgsScope.RESULTS.key(), lineItemsResult.getLineItemList());
            model.addAttribute(LtiAgsScope.DELETE_RESULTS.key(), deleteResult);
        }

        return LTIADVAGSMAIN;
    }

}
