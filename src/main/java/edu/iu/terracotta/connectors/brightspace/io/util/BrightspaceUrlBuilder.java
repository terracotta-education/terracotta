package edu.iu.terracotta.connectors.brightspace.io.util;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

import edu.iu.terracotta.connectors.brightspace.io.model.enums.api.BrightspaceUrl;

import java.util.List;

@Slf4j
@UtilityClass
@SuppressWarnings("PMD.GuardLogStatement")
public class BrightspaceUrlBuilder {

    public static String buildUrl(String baseUrl, String path, Map<String, List<String>> parameters) {
        return String.format(
            "%s%s",
            String.format(
                BrightspaceUrl.API_ROOT.url(),
                baseUrl,
                path
            ),
            HttpParameterBuilder.buildParameters(parameters)
        );
    }

}
