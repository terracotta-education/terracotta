package edu.iu.terracotta.connectors.generic.service.connector.impl;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections4.MapUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.stereotype.Service;

import edu.iu.terracotta.connectors.generic.annotation.TerracottaConnector;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.PlatformDeployment;
import edu.iu.terracotta.connectors.generic.dao.model.enums.LmsConnector;
import edu.iu.terracotta.connectors.generic.dao.repository.lti.PlatformDeploymentRepository;
import edu.iu.terracotta.connectors.generic.exceptions.TerracottaConnectorException;
import edu.iu.terracotta.connectors.generic.service.connector.ConnectorService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@SuppressWarnings({"unchecked", "PMD.GuardLogStatement"})
public class ConnectorServiceImpl<T> implements ConnectorService<T> {

    @Autowired private PlatformDeploymentRepository platformDeploymentRepository;
    @Autowired private ApplicationContext applicationContext;

    private Map<LmsConnector, Map<String, Object>> connectorServiceMap = new HashMap<>();

    @PostConstruct
    public void createConnectorMap() {
        // get classes annotated with "@TerracottaConnector(<LmsConnector>)"
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(TerracottaConnector.class));
        Set<BeanDefinition> connectorServices = scanner.findCandidateComponents("edu.iu.terracotta.connectors");

        // create mapping
        Arrays.stream(LmsConnector.values())
            .forEach(lmsConnector -> connectorServiceMap.put(lmsConnector, new HashMap<>()));

        // populated each connector service map (LmsConnector : {interface class: connector service impl})
        connectorServices.stream()
            .forEach(
                connector -> {
                    try {
                        // connector service impl annotation
                        TerracottaConnector terracottaConnector = Class.forName(connector.getBeanClassName()).getAnnotation(TerracottaConnector.class);

                        if (terracottaConnector == null) {
                            return;
                        }

                        // find interfaces of the impl that are annotated with "@TerracottaConnector(LmsConnector.GENERIC)"
                        Arrays.stream(Class.forName(connector.getBeanClassName()).getInterfaces())
                            .forEach(
                                iface -> {
                                    // find annotated interface
                                    TerracottaConnector ifaceTerracottaConnector = iface.getAnnotation(TerracottaConnector.class);

                                    if (ifaceTerracottaConnector == null) {
                                        return;
                                    }

                                    try {
                                        // add to connector services mapping
                                        Map<String, Object> connectorMap = connectorServiceMap.get(terracottaConnector.value());
                                        connectorMap.put(iface.getSimpleName(), applicationContext.getBean(Class.forName(connector.getBeanClassName())));
                                        connectorServiceMap.put(terracottaConnector.value(), connectorMap);
                                    } catch (ClassNotFoundException e) {
                                        log.error("No class with name: [{}] found.", connector.getBeanClassName(), e);
                                    }
                                }
                            );
                    } catch (ClassNotFoundException e) {
                        log.error("No class with name: [{}] found.", connector.getBeanClassName(), e);
                    }
                }
            );

            // log the connector initialization(s)
            connectorServiceMap.entrySet().stream()
                .filter(connectorService -> MapUtils.isNotEmpty(connectorService.getValue()))
                .forEach(
                    connectorService ->
                        log.info(
                            "Added {} connectors to services map: [{}]",
                            connectorService.getKey(),
                            connectorService.getValue().entrySet().stream()
                                .map(connector -> String.format("%s -> %s", connector.getValue().getClass().getSimpleName(), connector.getKey()))
                                .collect(Collectors.joining(", "))
                        )
                );
    }

    @Override
    public T instance(long platformDeploymentId, Class<?> type) throws TerracottaConnectorException {
        PlatformDeployment platformDeployment = platformDeploymentRepository.findById(platformDeploymentId)
            .orElseThrow(
                () -> new TerracottaConnectorException(
                    String.format(
                        "Error occurred attempting to to get connector service type [%s] for LMS Connector. PlatformDeployment ID: [%s] not found.",
                        type.getSimpleName(),
                        platformDeploymentId
                    )
                )
            );

        return instance(platformDeployment, type);
    }

    @Override
    public T instance(Optional<PlatformDeployment> platformDeployment, Class<?> type) throws TerracottaConnectorException {
        if (platformDeployment.isEmpty()) {
            throw new TerracottaConnectorException(
                String.format(
                    "Error occurred attempting to to get connector service type [%s] for LMS Connector. PlatformDeployment cannot be null.",
                    type.getSimpleName()
                )
            );
        }
        return instance(platformDeployment.get(), type);
    }

    @Override
    public T instance(PlatformDeployment platformDeployment, Class<?> type) throws TerracottaConnectorException {
        try {
            T service = (T) connectorServiceMap.get(platformDeployment.getLmsConnector()).get(type.getSimpleName());

            if (service != null) {
                return service;
            }

            throw new Exception(String.format("Connector service [%s] not found in map", type.getSimpleName()));
        } catch (Exception e) {
            log.error("Error occurred attempting to to get connector service type [{}] for LMS Connector: [{}]", type.getSimpleName(), platformDeployment.getLmsConnector(), e);
            throw new TerracottaConnectorException(
                String.format(
                    "Error occurred attempting to to get connector service type [%s] for LMS Connector: [%s]",
                    type.getSimpleName(),
                    platformDeployment.getLmsConnector()
                ),
                e
            );
        }
    }

}
