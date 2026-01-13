package edu.iu.terracotta.utils.lti;

import edu.iu.terracotta.utils.oauth.OAuthUtils;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.Jwts.SIG;
import lombok.experimental.UtilityClass;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.PlatformDeployment;
import edu.iu.terracotta.connectors.generic.service.lti.LtiDataService;
import edu.iu.terracotta.utils.LtiStrings;
import edu.iu.terracotta.utils.TextConstants;
import org.apache.commons.lang3.time.DateUtils;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@UtilityClass
@SuppressWarnings("PMD.LooseCoupling")
public final class DeepLinkUtils {

    public static Map<String, String> generateDeepLinkJWT(LtiDataService ltiDataService, PlatformDeployment platformDeployment, Lti3Request lti3Request, String localUrl) throws GeneralSecurityException, IOException {
        Date date = new Date();
        PrivateKey toolPrivateKey = OAuthUtils.loadPrivateKey(ltiDataService.getOwnPrivateKey());

        // JWT 1:  Empty list of JSON
        String jwt1 = Jwts.builder()
            .header()
            .add(LtiStrings.TYP, LtiStrings.JWT)
            .add(LtiStrings.KID, TextConstants.DEFAULT_KID)
            .add(LtiStrings.ALG, LtiStrings.RS256)
            .and()
            .issuer(platformDeployment.getClientId())  //Client ID
            .audience()
            .add(lti3Request.getIss())
            .and()
            .expiration(DateUtils.addSeconds(date, 3600)) //a java.util.Date
            .issuedAt(date) // for example, now
            .claim(LtiStrings.LTI_NONCE, lti3Request.getNonce())
            .claim(LtiStrings.LTI_AZP, lti3Request.getIss())
            .claim(LtiStrings.LTI_DEPLOYMENT_ID, lti3Request.getLtiDeploymentId())
            .claim(LtiStrings.LTI_MESSAGE_TYPE, LtiStrings.LTI_MESSAGE_TYPE_DEEP_LINKING_RESPONSE)
            .claim(LtiStrings.LTI_VERSION, LtiStrings.LTI_VERSION_3)
            .claim(LtiStrings.LTI_DATA, lti3Request.getDeepLinkData())
            .claim(LtiStrings.LTI_CONTENT_ITEMS, new HashMap<String, Object>())
            .signWith(toolPrivateKey, SIG.RS256)  //We sign it
            .compact();

        Map<String, String> deepLinkJwtMap = new HashMap<>();
        deepLinkJwtMap.put("jwt1", jwt1);

        //JWT 2: One ltiResourcelink
        List<Map<String, Object>> oneDeepLink = createOneDeepLinkWithGrades(localUrl);
        String jwt2 = Jwts.builder()
            .header()
            .add(LtiStrings.TYP, LtiStrings.JWT)
            .add(LtiStrings.KID, TextConstants.DEFAULT_KID)
            .add(LtiStrings.ALG, LtiStrings.RS256)
            .and()
            .issuer(platformDeployment.getClientId())  //Client ID
            .audience()
            .add(lti3Request.getIss())
            .and()
            .expiration(DateUtils.addSeconds(date, 3600)) //a java.util.Date
            .issuedAt(date) // for example, now
            .claim(LtiStrings.LTI_NONCE, lti3Request.getNonce())
            .claim(LtiStrings.LTI_AZP, lti3Request.getIss())
            .claim(LtiStrings.LTI_DEPLOYMENT_ID, lti3Request.getLtiDeploymentId())
            .claim(LtiStrings.LTI_MESSAGE_TYPE, LtiStrings.LTI_MESSAGE_TYPE_DEEP_LINKING_RESPONSE)
            .claim(LtiStrings.LTI_VERSION, LtiStrings.LTI_VERSION_3)
            .claim(LtiStrings.LTI_DATA, lti3Request.getDeepLinkData())
            .claim(LtiStrings.LTI_CONTENT_ITEMS, oneDeepLink)
            .signWith(toolPrivateKey, SIG.RS256)  //We sign it
            .compact();

        deepLinkJwtMap.put("jwt2", jwt2);
        deepLinkJwtMap.put("jwt2Map", listMapToJson(oneDeepLink));

        //JWT 2b: One link (not ltiResourcelink)
        List<Map<String, Object>> oneDeepLinkNoLti = createOneDeepLinkNoLti();
        String jwt2b = Jwts.builder()
            .header()
            .add(LtiStrings.TYP, LtiStrings.JWT)
            .add(LtiStrings.KID, TextConstants.DEFAULT_KID)
            .add(LtiStrings.ALG, LtiStrings.RS256)
            .and()
            .issuer(platformDeployment.getClientId())  //Client ID
            .audience()
            .add(lti3Request.getIss())
            .and()
            .expiration(DateUtils.addSeconds(date, 3600)) //a java.util.Date
            .issuedAt(date) // for example, now
            .claim(LtiStrings.LTI_NONCE, lti3Request.getNonce())
            .claim(LtiStrings.LTI_AZP, lti3Request.getIss())
            .claim(LtiStrings.LTI_DEPLOYMENT_ID, lti3Request.getLtiDeploymentId())
            .claim(LtiStrings.LTI_MESSAGE_TYPE, LtiStrings.LTI_MESSAGE_TYPE_DEEP_LINKING_RESPONSE)
            .claim(LtiStrings.LTI_VERSION, LtiStrings.LTI_VERSION_3)
            .claim(LtiStrings.LTI_DATA, lti3Request.getDeepLinkData())
            .claim(LtiStrings.LTI_CONTENT_ITEMS, oneDeepLinkNoLti)
            .signWith(toolPrivateKey, SIG.RS256)  //We sign it
            .compact();

        deepLinkJwtMap.put("jwt2b", jwt2b);
        deepLinkJwtMap.put("jwt2bMap", listMapToJson(oneDeepLinkNoLti));

        //JWT 3: More than one link
        List<Map<String, Object>> multipleDeepLink = createMultipleDeepLink(localUrl);
        String jwt3 = Jwts.builder()
            .header()
            .add(LtiStrings.TYP, LtiStrings.JWT)
            .add(LtiStrings.KID, TextConstants.DEFAULT_KID)
            .add(LtiStrings.ALG, LtiStrings.RS256)
            .and()
            .issuer(platformDeployment.getClientId())  //This is our own identifier, to know that we are the issuer.
            .audience()
            .add(lti3Request.getIss())
            .and()
            .expiration(DateUtils.addSeconds(date, 3600)) //a java.util.Date
            .issuedAt(date) // for example, now
            .claim(LtiStrings.LTI_NONCE, lti3Request.getNonce())
            .claim(LtiStrings.LTI_AZP, lti3Request.getIss())
            .claim(LtiStrings.LTI_DEPLOYMENT_ID, lti3Request.getLtiDeploymentId())
            .claim(LtiStrings.LTI_MESSAGE_TYPE, LtiStrings.LTI_MESSAGE_TYPE_DEEP_LINKING_RESPONSE)
            .claim(LtiStrings.LTI_VERSION, LtiStrings.LTI_VERSION_3)
            .claim(LtiStrings.LTI_DATA, lti3Request.getDeepLinkData())
            .claim(LtiStrings.LTI_CONTENT_ITEMS, multipleDeepLink)
            .signWith(toolPrivateKey, SIG.RS256)  //We sign it
            .compact();

        deepLinkJwtMap.put("jwt3", jwt3);
        deepLinkJwtMap.put("jwt3Map", listMapToJson(multipleDeepLink));

        //JWT 3b: More than one link but only ltiresourceLinks
        List<Map<String, Object>> multipleDeepLinkOnlyLti = createMultipleDeepLinkOnlyLti(localUrl);
        String jwt3b = Jwts.builder()
            .header()
            .add(LtiStrings.TYP, LtiStrings.JWT)
            .add(LtiStrings.KID, TextConstants.DEFAULT_KID)
            .add(LtiStrings.ALG, LtiStrings.RS256)
            .and()
            .issuer(platformDeployment.getClientId())  //This is our own identifier, to know that we are the issuer.
            .audience()
            .add(lti3Request.getIss())
            .and()
            .expiration(DateUtils.addSeconds(date, 3600)) //a java.util.Date
            .issuedAt(date) // for example, now
            .claim(LtiStrings.LTI_NONCE, lti3Request.getNonce())
            .claim(LtiStrings.LTI_AZP, lti3Request.getIss())
            .claim(LtiStrings.LTI_DEPLOYMENT_ID, lti3Request.getLtiDeploymentId())
            .claim(LtiStrings.LTI_MESSAGE_TYPE, LtiStrings.LTI_MESSAGE_TYPE_DEEP_LINKING_RESPONSE)
            .claim(LtiStrings.LTI_VERSION, LtiStrings.LTI_VERSION_3)
            .claim(LtiStrings.LTI_DATA, lti3Request.getDeepLinkData())
            .claim(LtiStrings.LTI_CONTENT_ITEMS, multipleDeepLinkOnlyLti)
            .signWith(toolPrivateKey, SIG.RS256)  //We sign it
            .compact();

        deepLinkJwtMap.put("jwt3b", jwt3b);
        deepLinkJwtMap.put("jwt3bMap", listMapToJson(multipleDeepLinkOnlyLti));

        return deepLinkJwtMap;
    }

    static List<Map<String, Object>> createOneDeepLink(String localUrl) {
        Map<String, Object> deepLink = new HashMap<>();
        deepLink.put(LtiStrings.DEEP_LINK_TYPE, LtiStrings.DEEP_LINK_LTIRESOURCELINK);
        deepLink.put(LtiStrings.DEEP_LINK_TITLE, "My test link");
        deepLink.put(LtiStrings.DEEP_LINK_URL, localUrl + "/lti3?link=1234");

        return Collections.singletonList(deepLink);
    }

    static List<Map<String, Object>> createOneDeepLinkWithGrades(String localUrl) {
        Map<String, Object> deepLink = new HashMap<>();
        deepLink.put(LtiStrings.DEEP_LINK_TYPE, LtiStrings.DEEP_LINK_LTIRESOURCELINK);
        deepLink.put(LtiStrings.DEEP_LINK_TITLE, "My test link");
        deepLink.put(LtiStrings.DEEP_LINK_URL, localUrl + "/lti3?link=1234");
        deepLink.put("lineItem", lineItem());

        Map<String, String> availableDates = new HashMap<>();
        Map<String, String> submissionDates = new HashMap<>();
        Map<String, String> custom = new HashMap<>();

        availableDates.put("startDateTime", "2018-03-07T20:00:03Z");
        availableDates.put("endDateTime", "2022-03-07T20:00:03Z");
        submissionDates.put("startDateTime", "2019-03-07T20:00:03Z");
        submissionDates.put("endDateTime", "2021-08-07T20:00:03Z");
        custom.put("dueDate", "$Resource.submission.endDateTime");
        custom.put("controlValue", "This is whatever I want to write here");
        deepLink.put("available", availableDates);
        deepLink.put("submission", submissionDates);
        deepLink.put("custom", custom);

        return Collections.singletonList(deepLink);
    }

    static Map<String, Object> lineItem() {
        Map<String, Object> deepLink = new HashMap<>();
        deepLink.put("scoreMaximum", 87);
        deepLink.put("label", "LTI 1234 Quiz");
        deepLink.put("resourceId", "1234");
        deepLink.put("tag", "myquiztest");

        return deepLink;
    }


    static List<Map<String, Object>> createOneDeepLinkNoLti() {
        Map<String, Object> deepLink2b = new HashMap<>();
        deepLink2b.put(LtiStrings.DEEP_LINK_TYPE, "link");
        deepLink2b.put(LtiStrings.DEEP_LINK_URL, "https://www.youtube.com/watch?v=corV3-WsIro");

        return Collections.singletonList(deepLink2b);
    }


    static List<Map<String, Object>> createMultipleDeepLink(String localUrl) {
        List<Map<String, Object>> deepLinks = createOneDeepLink(localUrl);

        Map<String, Object> deepLink2 = new HashMap<>();
        deepLink2.put(LtiStrings.DEEP_LINK_TYPE, "link");
        deepLink2.put(LtiStrings.DEEP_LINK_URL, "https://www.youtube.com/watch?v=corV3-WsIro");

        Map<String, Object> embed = new HashMap<>();
        embed.put("html", "<iframe width=\"560\" height=\"315\" src=\"https://www.youtube.com/embed/corV3-WsIro\" frameborder=\"0\" allow=\"autoplay; encrypted-media\" allowfullscreen></iframe>");
        deepLink2.put("embed", embed);

        Map<String, Object> window = new HashMap<>();
        window.put("targetName", "youtube-corV3-WsIro");
        window.put("windowFeatures", "height=560,width=315,menubar=no");
        deepLink2.put("window", window);

        Map<String, Object> iframe = new HashMap<>();
        iframe.put("src", "https://www.youtube.com/embed/corV3-WsIro");
        iframe.put("width", 560);
        iframe.put("height", 315);
        deepLink2.put("iframe", iframe);
        deepLinks.add(deepLink2);

        Map<String, Object> ltiResourceLink = new HashMap<>();
        ltiResourceLink.put(LtiStrings.DEEP_LINK_TYPE, LtiStrings.DEEP_LINK_LTIRESOURCELINK);
        ltiResourceLink.put(LtiStrings.DEEP_LINK_TITLE, "Another deep link");
        ltiResourceLink.put(LtiStrings.DEEP_LINK_URL, localUrl + "/lti3?link=4567");
        deepLinks.add(ltiResourceLink);


        Map<String, Object> deepLinkFilr = new HashMap<>();
        deepLinkFilr.put(LtiStrings.DEEP_LINK_TYPE, "file");
        deepLinkFilr.put(LtiStrings.DEEP_LINK_TITLE, "A file like a PDF that is my assignment submissions");
        deepLinkFilr.put(LtiStrings.DEEP_LINK_URL, "http://www.imsglobal.org/sites/default/files/ipr/imsipr_policyFinal.pdf");
        deepLinkFilr.put("mediaType", "application/pdf");
        deepLinks.add(deepLinkFilr);

        return deepLinks;
    }

    static List<Map<String, Object>> createMultipleDeepLinkOnlyLti(String localUrl) {
        Map<String, Object> ltiResourceLink = new HashMap<>();
        ltiResourceLink.put(LtiStrings.DEEP_LINK_TYPE, LtiStrings.DEEP_LINK_LTIRESOURCELINK);
        ltiResourceLink.put(LtiStrings.DEEP_LINK_TITLE, "Another deep link");
        ltiResourceLink.put(LtiStrings.DEEP_LINK_URL, localUrl + "/lti3?link=4567");

        return Collections.singletonList(ltiResourceLink);
    }

    private static String listMapToJson(List<Map<String, Object>> listMap) {
        try {
            return JsonMapper.builder()
                .build()
                .writerWithDefaultPrettyPrinter()
                .writeValueAsString(listMap);
        } catch (JacksonException e) {
            return "";
        }
    }

}
