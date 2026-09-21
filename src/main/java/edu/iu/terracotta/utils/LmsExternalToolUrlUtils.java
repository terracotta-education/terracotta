package edu.iu.terracotta.utils;

import java.net.URI;
import java.util.Arrays;
import java.util.Optional;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;

import lombok.experimental.UtilityClass;

/**
 * Parses the query-string parameters Terracotta's own LTI launch URLs carry (e.g.
 * ".../lti3?experiment=123&assignment=456") back out of an LMS assignment's external-tool URL -
 * shared by AssignmentAsyncServiceImpl (obsolete-assignment detection) and
 * ExperimentCopyCandidateServiceImpl (matching a Canvas-copied assignment back to the source-
 * course Terracotta assignment it was copied from).
 */
@UtilityClass
public class LmsExternalToolUrlUtils {

    // raw string - the caller decides how to interpret it. A Terracotta launch URL's assignment/
    // experiment query parameter is a legacy numeric id or a uuid depending on when the LMS-side
    // copy of the URL was created relative to the uuid migration (see resolveExperimentUuid's
    // permanent-dual-format comment in the LMS JWT services) - extractQueryParamAsLong below
    // covers only the numeric case, and is not what a caller expecting either format wants.
    public Optional<String> extractQueryParam(String url, String paramName) {
        if (StringUtils.isBlank(url)) {
            return Optional.empty();
        }

        String[] queryParameters = StringUtils.split(URI.create(url).getQuery(), '&');

        if (ArrayUtils.isEmpty(queryParameters)) {
            return Optional.empty();
        }

        return Arrays.stream(queryParameters)
            .filter(queryParameter -> Strings.CI.equals(StringUtils.split(queryParameter, '=')[0], paramName))
            .map(queryParameter -> StringUtils.split(queryParameter, '=')[1])
            .findFirst();
    }

    public Optional<Long> extractQueryParamAsLong(String url, String paramName) {
        return extractQueryParam(url, paramName).map(Long::parseLong);
    }

}
