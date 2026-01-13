package edu.iu.terracotta.utils.lti;

import com.google.common.hash.Hashing;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.AsymmetricJWK;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import edu.iu.terracotta.config.ApplicationConfig;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.LtiContextEntity;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.LtiLinkEntity;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.LtiMembershipEntity;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.LtiResultEntity;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.LtiUserEntity;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.PlatformDeployment;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.ToolDeployment;
import edu.iu.terracotta.connectors.generic.service.lti.LtiDataService;
import edu.iu.terracotta.connectors.generic.service.lti.impl.LtiDataServiceImpl;
import edu.iu.terracotta.exceptions.DataServiceException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Header;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwsHeader;
import io.jsonwebtoken.JwtParserBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.Locator;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;
import edu.iu.terracotta.utils.LtiStrings;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.thymeleaf.util.ListUtils;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.text.MessageFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * LTI3 Request object holds all the details for a valid LTI3 request
 *
 * Obtain this class using the static instance methods like so (recommended):
 * Lti3Request lti3Request = Lti3Request.getInstanceOrDie();
 *
 * Or by retrieving it from the HttpServletRequest attributes like so (best to not do this):
 * Lti3Request lti3Request = (Lti3Request) req.getAttribute(Lti3Request.class.getName());
 *
 * Devs may also need to use the LTIDataService service (injected) to access data when there is no
 * LTI request active.
 *
 * The main LTI data will also be placed into the Session and the Principal under the
 * LTI_USER_ID, LTI_CONTEXT_ID, and LTI_ROLE_ID constant keys.
 *
 */
@Slf4j
@Getter
@Setter
@SuppressWarnings({"PMD.GuardLogStatement", "ConstantConditions", "PMD.SingletonClassReturningNewInstance", "unchecked", "rawtypes", "PMD.LooseCoupling"})
public class Lti3Request {

    @Value("${app.lti.data.verbose.logging.enabled:false}")
    private boolean ltiDataVerboseLoggingEnabled;

    private HttpServletRequest httpServletRequest;
    private LtiDataService ltiDataService;

    // these are populated by the loadLTIDataFromDB operation

    private PlatformDeployment key;
    private ToolDeployment toolDeployment;
    private LtiContextEntity context;
    private LtiLinkEntity link;
    private LtiMembershipEntity membership;
    private LtiUserEntity user;
    private LtiResultEntity result;
    private boolean loaded = false;
    private boolean complete = false;
    private boolean correct = false;
    private boolean updated = false;
    private int loadingUpdates = 0;

    // these are populated on construct

    private String iss;
    private String aud;
    private Date iat;
    private Date exp;
    private String sub;
    private String kid;
    private String azp;

    private String ltiMessageType;
    private String ltiVersion;
    private String ltiDeploymentId;

    private String ltiGivenName;
    private String ltiFamilyName;
    private String ltiMiddleName;
    private String ltiPicture;
    private String ltiEmail;
    private String ltiName;

    private List<String> ltiRoles;
    private List<String> ltiRoleScopeMentor;
    private int userRoleNumber;
    private Map<String, Object> ltiResourceLink;
    private String ltiLinkId;
    private String ltiLinkTitle;
    private String ltiLinkDescription;

    private Map<String, Object> ltiContext;
    private String ltiContextId;
    private String ltiContextTitle;
    private String ltiContextLabel;
    private List<String> ltiContextType;

    private Map<String, Object> ltiToolPlatform;
    private String ltiToolPlatformName;
    private String ltiToolPlatformContactEmail;
    private String ltiToolPlatformDesc;
    private String ltiToolPlatformUrl;
    private String ltiToolPlatformProduct;
    private String ltiToolPlatformFamilyCode;
    private String ltiToolPlatformVersion;

    private Map<String, Object> ltiEndpoint;
    private List<String> ltiEndpointScope;
    private String ltiEndpointLineItems;

    private Map<String, Object> ltiNamesRoleService;
    private String ltiNamesRoleServiceContextMembershipsUrl;
    private List<String> ltiNamesRoleServiceVersions;

    private Map<String, Object> ltiCaliperEndpointService;
    private List<String> ltiCaliperEndpointServiceScopes;
    private String ltiCaliperEndpointServiceUrl;
    private String ltiCaliperEndpointServiceSessionId;

    private String lti11LegacyUserId;

    private String nonce;
    private String locale;

    private Map<String, Object> ltiLaunchPresentation;
    private String ltiPresTarget;
    private Integer ltiPresWidth;
    private Integer ltiPresHeight;
    private String ltiPresReturnUrl;
    private Locale ltiPresLocale;

    private Map<String, Object> ltiExtension;
    private Map<String, Object> ltiCustom;

    private Map<String, Object> deepLinkingSettings;
    private String deepLinkReturnUrl;
    private List<String> deepLinkAcceptTypes;
    private String deepLinkAcceptMediaTypes;
    private List<String> deepLinkAcceptPresentationDocumentTargets;
    private String deepLinkAcceptMultiple;
    private String deepLinkAutoCreate;
    private String deepLinkTitle;
    private String deepLinkText;
    private String deepLinkData;

    private String ltiTargetLinkUrl;

    private Map<String, Object> ltiLis;

    //DEEP LINKING RESPONSE (FOR DEMO PURPOSES_
    // We will return some hardcoded JWT's to test the deep Linking LTI Advanced Service standard, but the way this should work
    // is with the tool allowing the user to select the contents to link and generating the JWT with the selection

    private Map<String, String> deepLinkJwts;

    /**
     * @return the current Lti3Request object if there is one available, null if there isn't one and this is not a valid LTI3 based request
     */
    public static synchronized Lti3Request getInstance(String linkId) {
        Lti3Request ltiRequest = null;

        try {
            ltiRequest = getInstanceOrDie(linkId);
        } catch (Exception e) {
            log.debug("The method getInstanceOrDie... died", e);
        }

        return ltiRequest;
    }

    /**
     * @return the current Lti3Request object if there is one available
     * @throws IllegalStateException if the Lti3Request cannot be obtained
     */
    public static Lti3Request getInstanceOrDie(String linkId) {
        ServletRequestAttributes sra = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

        if (sra == null) {
            throw new IllegalStateException("No ServletRequestAttributes can be found, cannot get the LTIRequest unless we are currently in a request");
        }

        HttpServletRequest req = sra.getRequest();
        Lti3Request ltiRequest = (Lti3Request) req.getAttribute(Lti3Request.class.getName());

        if (ltiRequest == null) {
            log.debug("No LTIRequest found, attempting to create one for the current request");
            LtiDataService ltiDataService = null;

            try {
                ltiDataService = ApplicationConfig.getContext().getBean(LtiDataServiceImpl.class);
            } catch (Exception e) {
                log.warn("Unable to get the LTIDataService, initializing the LTIRequest without it");
            }

            try {
                if (ltiDataService != null) {
                    ltiRequest = new Lti3Request(req, ltiDataService, true, linkId);
                } else { //THIS SHOULD NOT HAPPEN
                    throw new IllegalStateException("Error internal, no Dataservice available: " + req);
                }
            } catch (Exception e) {
                log.warn("Failure trying to create the LTIRequest: ", e);
            }
        }

        if (ltiRequest == null) {
            throw new IllegalStateException("Invalid LTI request, cannot create LTIRequest from request: " + req);
        }

        return ltiRequest;
    }

    /**
     * @param request an http servlet request
     * @param ltiDataService   the service used for accessing LTI data
     * @param update  if true then update (or insert) the DB records for this request (else skip DB updating)
     * @throws IllegalStateException if this is not an LTI request
     */
    public Lti3Request(HttpServletRequest request, LtiDataService ltiDataService, boolean update, String linkId) throws DataServiceException {
        if (request == null) {
            throw new AssertionError("cannot make an LtiRequest without a request");
        }

        if (ltiDataService == null) {
            throw new AssertionError("LTIDataService cannot be null");
        }

        this.ltiDataService = ltiDataService;
        this.httpServletRequest = request;
        // extract the typical LTI data from the request
        String jwt = httpServletRequest.getParameter("id_token");
        JwtParserBuilder parser = Jwts.parser();
        String[] jwtSections = jwt.split("\\.");
        String jwtPayload = new String(Base64.getUrlDecoder().decode(jwtSections[1]));
        Map<String, Object> jwtClaims = null;

        try {
            jwtClaims = JsonMapper.builder()
                .build()
                .readValue(
                    jwtPayload,
                    new TypeReference<Map<String,Object>>() {}
                );
        } catch (JacksonException e) {
            throw new IllegalStateException("Request is not a valid LTI3 request.", e);
        }

        String audience = (String) jwtClaims.get(LtiStrings.AUD);
        String issuer = (String) jwtClaims.get(LtiStrings.ISS);

        parser.keyLocator(
            new Locator<Key>() {
                @Override
                public Key locate(Header header) {
                    if (header instanceof JwsHeader) {
                        JwsHeader jwsHeader = (JwsHeader) header;
                        PlatformDeployment platformDeployment = ltiDataService.getPlatformDeploymentRepository().findByIssAndClientId(issuer, audience).get(0);

                        if (StringUtils.isEmpty(platformDeployment.getJwksEndpoint())) {
                            log.error("The platform configuration must contain a Jwks endpoint");
                            return null;
                        }

                        try {
                            JWKSet publicKeys = JWKSet.load(new URI(platformDeployment.getJwksEndpoint()).toURL());
                            JWK jwk = publicKeys.getKeyByKeyId(jwsHeader.getKeyId());

                            return ((AsymmetricJWK) jwk).toPublicKey();
                        } catch (JOSEException | ParseException | IOException | URISyntaxException ex) {
                            log.error("Error getting the iss public key", ex);
                            return null;
                        }
                    }

                    return null;
                }
            }
        );

        Jws<Claims> jws = parser.build().parseSignedClaims(jwt);
        //This is just for logging.
        if (ltiDataVerboseLoggingEnabled) {
            Enumeration<String> sessionAttributes = httpServletRequest.getSession().getAttributeNames();
            log.debug("----------------------BEFORE---------------------------------------------------------------------------------");

            while (sessionAttributes.hasMoreElements()) {
                String attName = sessionAttributes.nextElement();
                log.debug(attName + " : " + httpServletRequest.getSession().getAttribute(attName));
            }

            log.debug("-------------------------------------------------------------------------------------------------------");
        }

        //We check that the LTI request is a valid LTI Request and has the right type.
        String isLti3Request = isLti3Request(jws);

        if (!(LtiStrings.LTI_MESSAGE_TYPE_RESOURCE_LINK.equals(isLti3Request) || LtiStrings.LTI_MESSAGE_TYPE_DEEP_LINKING.equals(isLti3Request))) {
            throw new IllegalStateException("Request is not a valid LTI3 request: " + isLti3Request);
        }

        //Now we are going to check the if the nonce is valid.
        String checkNonce = checkNonce(jws);

        if (!BooleanUtils.toBoolean(checkNonce)) {
            throw new IllegalStateException("Nonce error: " + checkNonce);
        }

        //Here we will populate the Lti3Request object
        String processRequestParameters = processRequestParameters(request, jws);

        if (!BooleanUtils.toBoolean(processRequestParameters)) {
            throw new IllegalStateException("Request is not a valid LTI3 request: " + processRequestParameters);
        }

        // We update the database in case we have new values. (New users, new resources...etc)
        if (isLti3Request.equals(LtiStrings.LTI_MESSAGE_TYPE_RESOURCE_LINK) || isLti3Request.equals(LtiStrings.LTI_MESSAGE_TYPE_DEEP_LINKING)) {
            //Load data from DB related with this request and update it if needed with the new values.
            ToolDeployment toolDeployment = ltiDataService.findOrCreateToolDeployment(this.iss, this.aud, this.ltiDeploymentId);

            if (toolDeployment == null) {
                throw new IllegalStateException(
                        MessageFormat.format("Could not find a tool deployment for iss: {0}, clientId: {1}, ltiDeploymentId: {2}",
                                this.iss, this.aud, this.ltiDeploymentId));
            }

            ltiDataService.loadLTIDataFromDB(this, linkId);

            if (update) {
                if (isLti3Request.equals(LtiStrings.LTI_MESSAGE_TYPE_RESOURCE_LINK)) {
                    ltiDataService.upsertLTIDataInDB(this, toolDeployment, linkId);
                } else {
                    ltiDataService.upsertLTIDataInDB(this, toolDeployment, null);
                }
            }
        }
    }

    /**
     * Processes all the parameters in this request into populated internal variables in the LTI Request
     *
     * @param request an http servlet request
     * @return true if this is a complete and correct LTI request (includes key, context, link, user) OR false otherwise
     */
    public String processRequestParameters(HttpServletRequest request, Jws<Claims> jws) {
        if (request != null && this.httpServletRequest != request) {
            this.httpServletRequest = request;
        }

        assert this.httpServletRequest != null;

        iss = jws.getPayload().getIssuer();
        aud = jws.getPayload().getAudience().toArray(new String[jws.getPayload().getAudience().size()])[0];
        iat = jws.getPayload().getIssuedAt();
        exp = jws.getPayload().getExpiration();
        sub = jws.getPayload().getSubject();
        nonce = getStringFromLTIRequest(jws, LtiStrings.LTI_NONCE);
        azp = getStringFromLTIRequest(jws, LtiStrings.LTI_AZP);

        ltiMessageType = getStringFromLTIRequest(jws, LtiStrings.LTI_MESSAGE_TYPE);
        ltiVersion = getStringFromLTIRequest(jws, LtiStrings.LTI_VERSION);
        ltiDeploymentId = getStringFromLTIRequest(jws, LtiStrings.LTI_DEPLOYMENT_ID);

        ltiGivenName = getStringFromLTIRequest(jws, LtiStrings.LTI_GIVEN_NAME);
        ltiFamilyName = getStringFromLTIRequest(jws, LtiStrings.LTI_FAMILY_NAME);
        ltiMiddleName = getStringFromLTIRequest(jws, LtiStrings.LTI_MIDDLE_NAME);
        ltiPicture = getStringFromLTIRequest(jws, LtiStrings.LTI_PICTURE);

        ltiEmail = getStringFromLTIRequest(jws, LtiStrings.LTI_EMAIL);
        ltiName = getStringFromLTIRequest(jws, LtiStrings.LTI_NAME);

        ltiRoles = getListFromLTIRequest(jws, LtiStrings.LTI_ROLES);
        userRoleNumber = makeUserRoleNum(ltiRoles);
        ltiRoleScopeMentor = getListFromLTIRequest(jws, LtiStrings.LTI_ROLE_SCOPE_MENTOR);

        ltiResourceLink = getMapFromLTIRequest(jws, LtiStrings.LTI_LINK);
        ltiLinkId = getStringFromLTIRequestMap(ltiResourceLink, LtiStrings.LTI_LINK_ID);
        ltiLinkDescription = getStringFromLTIRequestMap(ltiResourceLink, LtiStrings.LTI_LINK_DESC);
        ltiLinkTitle = getStringFromLTIRequestMap(ltiResourceLink, LtiStrings.LTI_LINK_TITLE);

        ltiContext = getMapFromLTIRequest(jws, LtiStrings.LTI_CONTEXT);
        ltiContextId = getStringFromLTIRequestMap(ltiContext, LtiStrings.LTI_CONTEXT_ID);
        ltiContextLabel = getStringFromLTIRequestMap(ltiContext, LtiStrings.LTI_CONTEXT_LABEL);
        ltiContextTitle = getStringFromLTIRequestMap(ltiContext, LtiStrings.LTI_CONTEXT_TITLE);
        ltiContextType = getListFromLTIRequestMap(ltiContext, LtiStrings.LTI_CONTEXT_TYPE);

        ltiToolPlatform = getMapFromLTIRequest(jws, LtiStrings.LTI_PLATFORM);
        ltiToolPlatformName = getStringFromLTIRequestMap(ltiToolPlatform, LtiStrings.LTI_PLATFORM_NAME);
        ltiToolPlatformContactEmail = getStringFromLTIRequestMap(ltiToolPlatform, LtiStrings.LTI_PLATFORM_CONTACT_EMAIL);
        ltiToolPlatformDesc = getStringFromLTIRequestMap(ltiToolPlatform, LtiStrings.LTI_PLATFORM_DESC);
        ltiToolPlatformUrl = getStringFromLTIRequestMap(ltiToolPlatform, LtiStrings.LTI_PLATFORM_URL);
        ltiToolPlatformProduct = getStringFromLTIRequestMap(ltiToolPlatform, LtiStrings.LTI_PLATFORM_PRODUCT);
        ltiToolPlatformFamilyCode = getStringFromLTIRequestMap(ltiToolPlatform, LtiStrings.LTI_PLATFORM_PRODUCT_FAMILY_CODE);
        ltiToolPlatformVersion = getStringFromLTIRequestMap(ltiToolPlatform, LtiStrings.LTI_PLATFORM_VERSION);

        ltiEndpoint = getMapFromLTIRequest(jws, LtiStrings.LTI_ENDPOINT);
        ltiEndpointScope = getListFromLTIRequestMap(ltiEndpoint, LtiStrings.LTI_ENDPOINT_SCOPE);
        ltiEndpointLineItems = getStringFromLTIRequestMap(ltiEndpoint, LtiStrings.LTI_ENDPOINT_LINEITEMS);

        ltiNamesRoleService = getMapFromLTIRequest(jws, LtiStrings.LTI_NAMES_ROLE_SERVICE);
        ltiNamesRoleServiceContextMembershipsUrl = getStringFromLTIRequestMap(ltiNamesRoleService, LtiStrings.LTI_NAMES_ROLE_SERVICE_CONTEXT);
        ltiNamesRoleServiceVersions = getListFromLTIRequestMap(ltiNamesRoleService, LtiStrings.LTI_NAMES_ROLE_SERVICE_VERSIONS);

        ltiCaliperEndpointService = getMapFromLTIRequest(jws, LtiStrings.LTI_CALIPER_ENDPOINT_SERVICE);
        ltiCaliperEndpointServiceScopes = getListFromLTIRequestMap(ltiCaliperEndpointService, LtiStrings.LTI_CALIPER_ENDPOINT_SERVICE_SCOPES);
        ltiCaliperEndpointServiceUrl = getStringFromLTIRequestMap(ltiCaliperEndpointService, LtiStrings.LTI_CALIPER_ENDPOINT_SERVICE_URL);
        ltiCaliperEndpointServiceSessionId = getStringFromLTIRequestMap(ltiCaliperEndpointService, LtiStrings.LTI_CALIPER_ENDPOINT_SERVICE_SESSION_ID);

        ltiLis = getMapFromLTIRequest(jws, LtiStrings.LTI_LIS);


        lti11LegacyUserId = getStringFromLTIRequest(jws, LtiStrings.LTI_11_LEGACY_USER_ID);

        locale = getStringFromLTIRequest(jws, LtiStrings.LTI_PRES_LOCALE);

        if (locale == null) {
            ltiPresLocale = Locale.getDefault();
        } else {
            ltiPresLocale = Locale.forLanguageTag(locale);
        }

        ltiLaunchPresentation = getMapFromLTIRequest(jws, LtiStrings.LTI_LAUNCH_PRESENTATION);
        ltiPresHeight = getIntegerFromLTIRequestMap(ltiLaunchPresentation, LtiStrings.LTI_PRES_HEIGHT);
        ltiPresWidth = getIntegerFromLTIRequestMap(ltiLaunchPresentation, LtiStrings.LTI_PRES_WIDTH);
        ltiPresReturnUrl = getStringFromLTIRequestMap(ltiLaunchPresentation, LtiStrings.LTI_PRES_RETURN_URL);
        ltiPresTarget = getStringFromLTIRequestMap(ltiLaunchPresentation, LtiStrings.LTI_PRES_TARGET);

        ltiCustom = getMapFromLTIRequest(jws, LtiStrings.LTI_CUSTOM);
        ltiExtension = getMapFromLTIRequest(jws, LtiStrings.LTI_EXTENSION);

        ltiTargetLinkUrl = getStringFromLTIRequest(jws, LtiStrings.LTI_TARGET_LINK_URI);

        //LTI3 DEEP LINKING

        deepLinkingSettings = getMapFromLTIRequest(jws, LtiStrings.DEEP_LINKING_SETTINGS);
        deepLinkReturnUrl = getStringFromLTIRequestMap(deepLinkingSettings, LtiStrings.DEEP_LINK_RETURN_URL);
        deepLinkAcceptTypes = getListFromLTIRequestMap(deepLinkingSettings, LtiStrings.DEEP_LINK_ACCEPT_TYPES);
        deepLinkAcceptMediaTypes = getStringFromLTIRequestMap(deepLinkingSettings, LtiStrings.DEEP_LINK_ACCEPT_MEDIA_TYPES);
        deepLinkAcceptPresentationDocumentTargets = getListFromLTIRequestMap(deepLinkingSettings, LtiStrings.DEEP_LINK_DOCUMENT_TARGETS);
        deepLinkAcceptMultiple = getStringFromLTIRequestMap(deepLinkingSettings, LtiStrings.DEEP_LINK_ACCEPT_MULTIPLE);
        deepLinkAutoCreate = getStringFromLTIRequestMap(deepLinkingSettings, LtiStrings.DEEP_LINK_AUTO_CREATE);
        deepLinkTitle = getStringFromLTIRequestMap(deepLinkingSettings, LtiStrings.DEEP_LINK_TITLE);
        deepLinkText = getStringFromLTIRequestMap(deepLinkingSettings, LtiStrings.DEEP_LINK_TEXT);
        deepLinkData = getStringFromLTIRequestMap(deepLinkingSettings, LtiStrings.DEEP_LINK_DATA);

        // A sample that shows how we can store some of this in the session
        HttpSession session = this.httpServletRequest.getSession();
        session.setAttribute(LtiStrings.LTI_SESSION_USER_ID, sub);
        session.setAttribute(LtiStrings.LTI_SESSION_CONTEXT_ID, ltiContextId);
        session.setAttribute(LtiStrings.LTI_SESSION_CONTEXT_ID, ltiContextId);

        try {
            ToolDeployment toolDeployment = this.ltiDataService.findOrCreateToolDeployment(iss, aud, ltiDeploymentId);
            session.setAttribute(LtiStrings.LTI_SESSION_TOOL_DEPLOYMENT_ID, toolDeployment.getLtiDeploymentId());
        } catch (Exception e) {
            log.error("No deployment found");
        }

        // Surely we need a more elaborated code here based in the huge amount of roles available.
        // In any case, this is for the session... we still have the full list of roles in the ltiRoles list

        session.setAttribute(LtiStrings.LTI_SESSION_USER_ROLE, getNormalizedRoleName());

        // And now we will check that all the mandatory fields are there and are correct
        String isComplete;
        String isCorrect;

        if (LtiStrings.LTI_MESSAGE_TYPE_RESOURCE_LINK.equals(ltiMessageType)) {
            isComplete = checkCompleteLTIRequest();
            complete = BooleanUtils.toBoolean(isComplete);
            isCorrect = checkCorrectLTIRequest();
            correct = BooleanUtils.toBoolean(isCorrect);
        } else {  // DEEP Linking
            isComplete = checkCompleteDeepLinkingRequest();
            complete = BooleanUtils.toBoolean(isComplete);
            isCorrect = checkCorrectDeepLinkingRequest();
            correct = BooleanUtils.toBoolean(isCorrect);

            // NOTE: This is just to hardcode some demo information.
            try {
                deepLinkJwts = DeepLinkUtils.generateDeepLinkJWT(
                    ltiDataService,
                    ltiDataService.getPlatformDeploymentRepository().findByToolDeployments_LtiDeploymentId(ltiDeploymentId).get(0),
                    this,
                    toolDeployment.getPlatformDeployment().getLocalUrl());
            } catch (GeneralSecurityException | IOException | NullPointerException ex) {
                log.error("Error creating the DeepLinking Response", ex);
            }
        }

        // This is an ugly way to display the error... can be improved.
        if (complete && correct) {
            return Boolean.TRUE.toString();
        }

        if (complete) {
            isComplete = "";
        } else if (correct) {
            isCorrect = "";
        }

        return isComplete + isCorrect;
    }

    private String getNormalizedRoleName() {
        if (isRoleAdministrator()) {
            return LtiStrings.LTI_ROLE_ADMIN;
        } else if (isRoleInstructor()) {
            return LtiStrings.LTI_ROLE_MEMBERSHIP_INSTRUCTOR;
        } else if (isRoleLearner()) {
            return LtiStrings.LTI_ROLE_MEMBERSHIP_LEARNER;
        }

        return LtiStrings.LTI_ROLE_GENERAL;
    }

    private String getStringFromLTIRequest(Jws<Claims> jws, String stringToGet) {
        if (jws.getPayload().containsKey(stringToGet) && jws.getPayload().get(stringToGet) != null) {
            return jws.getPayload().get(stringToGet, String.class);
        }

        return null;
    }

    private String getStringFromLTIRequestMap(Map<String, Object> map, String stringToGet) {
        if (map.containsKey(stringToGet) && map.get(stringToGet) != null) {
            return map.get(stringToGet).toString();
        }

            return null;
    }

    private Integer getIntegerFromLTIRequestMap(Map<String, Object> map, String integerToGet) {
        if (map.containsKey(integerToGet)) {
            try {
                return Integer.valueOf(map.get(integerToGet).toString());
            } catch (Exception ex) {
                log.error("No integer when expected in: {0}. Returning null", integerToGet);
                return null;
            }
        }

        return null;
    }

    private List<String> getListFromLTIRequestMap(Map<String, Object> map, String listToGet) {
        if (map.containsKey(listToGet)) {
            try {
                return (List<String>) map.get(listToGet);
            } catch (Exception ex) {
                log.error("No list when expected in: {0} Returning null", listToGet);
                return new ArrayList<>();
            }
        }

        return new ArrayList<>();
    }

    private Map<String, Object> getMapFromLTIRequest(Jws<Claims> jws, String mapToGet) {
        if (jws.getPayload().containsKey(mapToGet)) {
            try {
                return jws.getPayload().get(mapToGet, Map.class);
            } catch (Exception ex) {
                log.error("No map integer when expected in: {0}. Returning null", mapToGet);
                return new HashMap<>();
            }
        }

        return new HashMap<>();
    }

    private List<String> getListFromLTIRequest(Jws<Claims> jws, String listToGet) {
        if (jws.getPayload().containsKey(listToGet)) {
            try {
                return jws.getPayload().get(listToGet, List.class);
            } catch (Exception ex) {
                log.error("No map integer when expected in: '{}'. Returning null", listToGet);
                return new ArrayList<>();
            }
        }

        return new ArrayList<>();
    }

    /**
     * Checks if this LTI request object has a complete set of required LTI data,
     * also sets the #complete variable appropriately
     *
     * @param objects if true then check for complete objects, else just check for complete request params
     * @return true if complete
     */
    public boolean checkCompleteLTIRequest(boolean objects) {
        return objects && key != null && context != null && link != null && user != null;
    }

    /**
     * Checks if this LTI3 request object has a complete set of required LTI3 data,
     * NOTE: this code is not the one I would create for production, it is more a didactic one
     * to understand what is being checked.
     *
     * @return true if complete
     */
    public String checkCompleteLTIRequest() {
        String completeStr = "";

        if (StringUtils.isEmpty(ltiDeploymentId)) {
            completeStr += " Lti Deployment Id is empty.\n ";
        }

        if (MapUtils.isEmpty(ltiResourceLink)) {
            completeStr += " Lti Resource Link is empty.\n ";
        } else {
            if (StringUtils.isEmpty(ltiLinkId)) {
                completeStr += " Lti Resource Link ID is empty.\n ";
            }
        }

        if (StringUtils.isEmpty(sub)) {
            completeStr += " User (sub) is empty.\n ";
        }

        if (ltiRoles == null || ListUtils.isEmpty(ltiRoles)) {
            completeStr += " Lti Roles is empty.\n ";
        }

        if (exp == null) {
            completeStr += " Exp is empty or invalid.\n ";
        }

        if (iat == null) {
            completeStr += " Iat is empty or invalid.\n ";
        }

        if (StringUtils.isBlank(completeStr)) {
            return "true";
        } else {
            return completeStr;
        }
    }

    /**
     * Checks if this Deep Linking request object has a complete set of required LTI3 data,
     * NOTE: this code is not the one I would create for production, it is more a didactic one
     * to understand what is being checked.
     *
     * @return true if complete
     */

    public String checkCompleteDeepLinkingRequest() {
        String completeStr = "";

        if (StringUtils.isEmpty(ltiDeploymentId)) {
            completeStr += " Lti Deployment Id is empty.\n ";
        }

        if (StringUtils.isEmpty(sub)) {
            completeStr += " User (sub) is empty.\n ";
        }

        if (exp == null) {
            completeStr += " Exp is empty or invalid.\n ";
        }

        if (iat == null) {
            completeStr += " Iat is empty or invalid.\n ";
        }

        if (MapUtils.isEmpty(deepLinkingSettings)) {
            completeStr += " DeepLinkingSettings is empty or invalid.\n ";
        }

        if (StringUtils.isEmpty(deepLinkReturnUrl)) {
            completeStr += " deepLinkReturnUrl is empty.\n ";
        }

        if (CollectionUtils.isEmpty(deepLinkAcceptTypes)) {
            completeStr += " deepLink AcceptTypes is empty.\n ";
        }

        if (CollectionUtils.isEmpty(deepLinkAcceptPresentationDocumentTargets)) {
            completeStr += " deepLink AcceptPresentationDocumentTargets is empty.\n ";
        }

        if (StringUtils.isBlank(completeStr)) {
            return "true";
        }

        return completeStr;
    }

    /**
     * Checks if this LTI3 request object has correct values
     *
     * @return the string "true" if complete and the error message if not
     */
    private String checkCorrectLTIRequest() {
        return Boolean.TRUE.toString();
    }

    /**
     * Checks if this Deep Linking request object has correct values
     *
     * @return the string "true" if complete and the error message if not
     */
    private String checkCorrectDeepLinkingRequest() {
        return Boolean.TRUE.toString();
    }

    /**
     * @param jws the JWT token parsed.
     * @return true if this is a valid LTI request
     */
    public String checkNonce(Jws<Claims> jws) {
        //We get all the nonces from the session, and compare.
        List<String> ltiNonce = (List) httpServletRequest.getSession().getAttribute("lti_nonce");
        List<String> ltiNonceNew = new ArrayList<>();
        boolean found = false;
        String nonceToCheck = jws.getPayload().get(LtiStrings.LTI_NONCE, String.class);

        if (nonceToCheck == null || ListUtils.isEmpty(ltiNonce)) {
            return "Nonce = null in the JWT or in the session.";
        } else {
            // Really, we send the hash of the nonce to the platform.
            for (String nonceStored : ltiNonce) {
                String nonceHash = Hashing.sha256()
                    .hashString(nonceStored, StandardCharsets.UTF_8)
                    .toString();

                if (nonceToCheck.equals(nonceHash)) {
                    found = true;
                } else { //If not found, we add it to another list... so we keep the unused nonces.
                    ltiNonceNew.add(nonceStored);
                }
            }
            if (found) {
                httpServletRequest.getSession().setAttribute("lti_nonce", ltiNonceNew);
                return Boolean.TRUE.toString();
            } else {
                return "Unknown or already used nounce.";
            }
        }
    }

    /**
     * @param jws the JWT token parsed.
     * @return true if this is a valid LTI request
     */
    public static String isLti3Request(Jws<Claims> jws) {
        String errorDetail = "";
        boolean valid = false;
        String ltiVersion = jws.getPayload().get(LtiStrings.LTI_VERSION, String.class);

        if (ltiVersion == null) {
            errorDetail = "LTI Version = null. ";
        }

        String ltiMessageType = jws.getPayload().get(LtiStrings.LTI_MESSAGE_TYPE, String.class);

        if (ltiMessageType == null) {
            errorDetail += "LTI Message Type = null. ";
        }

        if (ltiMessageType != null && ltiVersion != null) {
            boolean goodMessageType = LtiStrings.LTI_MESSAGE_TYPE_RESOURCE_LINK.equals(ltiMessageType) || LtiStrings.LTI_MESSAGE_TYPE_DEEP_LINKING.equals(ltiMessageType);

            if (!goodMessageType) {
                errorDetail = "LTI Message Type is not right: " + ltiMessageType + ". ";
            }

            boolean goodLTIVersion = LtiStrings.LTI_VERSION_3.equals(ltiVersion);

            if (!goodLTIVersion) {
                errorDetail += "LTI Version is not right: " + ltiVersion;
            }

            valid = goodMessageType && goodLTIVersion;
        }

        if (valid && LtiStrings.LTI_MESSAGE_TYPE_RESOURCE_LINK.equals(ltiMessageType)) {
            return LtiStrings.LTI_MESSAGE_TYPE_RESOURCE_LINK;
        } else if (valid && LtiStrings.LTI_MESSAGE_TYPE_DEEP_LINKING.equals(ltiMessageType)) {
            return LtiStrings.LTI_MESSAGE_TYPE_DEEP_LINKING;
        }

        return errorDetail;
    }

    public boolean isRoleAdministrator() {
        return ltiRoles != null && userRoleNumber >= 2;
    }

    public boolean isRoleInstructor() {
        return ltiRoles != null && userRoleNumber >= 1;
    }

    public boolean isRoleLearner() {
        return CollectionUtils.containsAny(ltiRoles, LtiStrings.LTI_ROLE_MEMBERSHIP_LEARNER);
    }

    /**
     * @param rawUserRoles the raw roles string (this could also only be part of the string assuming it is the highest one)
     * @return the number that represents the role (higher is more access)
     */
    public int makeUserRoleNum(List<String> rawUserRoles) {
        if (rawUserRoles != null) {
            if (rawUserRoles.contains(LtiStrings.LTI_ROLE_MEMBERSHIP_ADMIN)) {
                return 2;
            } else if (rawUserRoles.contains(LtiStrings.LTI_ROLE_MEMBERSHIP_INSTRUCTOR)) {
                return 1;
            }
        }

        return 0;
    }

}
