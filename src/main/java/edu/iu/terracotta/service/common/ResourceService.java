package edu.iu.terracotta.service.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public interface ResourceService<T> {

    default Set<T> getResources(Class<T> objectClass) throws IOException {
        Resource[] resources = new PathMatchingResourcePatternResolver(this.getClass().getClassLoader()).getResources(getDirectoryPath() + "/*.json");
        Set<T> objects = new HashSet<>();

        for (Resource resource : resources) {
            T object = new ObjectMapper().readValue(resource.getInputStream(), objectClass);
            objects.add(object);
        }

        return objects;
    }

    String getDirectoryPath();

    void setDefaults();

}
