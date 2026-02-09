package edu.iu.terracotta.connectors.brightspace.io.util;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.MapUtils;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

/*
 * This class transforms a map into a parameter string
 *
 * EX: '{ array[]: [a, b], item: [c] } would return the following string
 * ?array[]=a&array[]=b&item=c
 */
@Slf4j
@UtilityClass
@SuppressWarnings("PMD.GuardLogStatement")
public class HttpParameterBuilder {

    public static String buildParameters(Map<String, List<String>> parameters) {
        if (MapUtils.isEmpty(parameters)) {
            return "";
        }

        return parameters.entrySet().stream()
            .map(HttpParameterBuilder::buildParameter)
            .reduce((a, b) -> a + b)
            .filter(s -> s.length() > 0)
            .map(s -> s.substring(1))
            .map(paramString -> "?" + paramString)
            .orElse("");
    }

    private static String buildParameter(Map.Entry<String, List<String>> entry) {
        return entry.getValue()
            .stream()
            .reduce("", (a, paramValue) -> {
                return a + "&" + URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8)
                    + "=" + URLEncoder.encode(paramValue, StandardCharsets.UTF_8);
            });
    }

}
