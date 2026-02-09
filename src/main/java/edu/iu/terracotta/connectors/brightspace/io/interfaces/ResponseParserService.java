package edu.iu.terracotta.connectors.brightspace.io.interfaces;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import edu.iu.terracotta.connectors.brightspace.io.net.model.Response;
import tools.jackson.core.type.TypeReference;

@SuppressWarnings({"PMD.LooseCoupling"})
public interface ResponseParserService {

    <T> List<T> parseToList(TypeReference<List<T>> typeReference, Response response);
    <T> Optional<T> parseToObject(Class<T> clazz, Response response);
    <T> Map<String, T> parseToMap(Class<T> clazz, Response response);

}
