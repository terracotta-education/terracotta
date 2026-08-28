package edu.iu.terracotta.connectors.generic.service.connector.impl;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.collections4.MapUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import edu.iu.terracotta.connectors.generic.annotation.TerracottaConnector;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.PlatformDeployment;
import edu.iu.terracotta.connectors.generic.dao.model.enums.LmsConnector;
import edu.iu.terracotta.connectors.generic.dao.repository.lti.PlatformDeploymentRepository;
import edu.iu.terracotta.connectors.generic.exceptions.TerracottaConnectorException;
import edu.iu.terracotta.connectors.generic.service.connector.ConnectorService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@SuppressWarnings({"unchecked", "PMD.GuardLogStatement"})
public class ConnectorServiceImpl<T> implements ConnectorService<T> {

    private final PlatformDeploymentRepository platformDeploymentRepository;
    private final ApplicationContext applicationContext;

    private Map<LmsConnector, Map<String, Object>> connectorServiceMap = new HashMap<>();

    @PostConstruct
    public void createConnectorMap() {
        Arrays.stream(LmsConnector.values())
            .forEach(lmsConnector -> connectorServiceMap.put(lmsConnector, new HashMap<>()));

        // beans annotated with "@TerracottaConnector(<LmsConnector>)" - Spring already tracks this
        // via bean definition metadata, so no need to re-scan the classpath by hand
        Arrays.stream(applicationContext.getBeanNamesForAnnotation(TerracottaConnector.class))
            .forEach(this::registerConnectorBean);

        logConnectorMap();
    }

    // getType() (rather than beanName -> getBean() -> getClass()) resolves the actual declared
    // bean type even when Spring has wrapped it in a CGLIB proxy (e.g. for @Transactional
    // connector impls) - TerracottaConnector isn't @Inherited, so the proxy subclass itself
    // wouldn't carry the annotation.
    private void registerConnectorBean(String beanName) {
        Class<?> beanType = applicationContext.getType(beanName);

        if (beanType == null) {
            return;
        }

        TerracottaConnector terracottaConnector = beanType.getAnnotation(TerracottaConnector.class);

        if (terracottaConnector == null) {
            return;
        }

        // find interfaces of the impl that are annotated with "@TerracottaConnector(LmsConnector.GENERIC)"
        Arrays.stream(beanType.getInterfaces())
            .filter(iface -> iface.getAnnotation(TerracottaConnector.class) != null)
            .forEach(iface -> connectorServiceMap.get(terracottaConnector.value()).put(iface.getSimpleName(), applicationContext.getBean(beanName)));
    }

    private void logConnectorMap() {
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
                        "Error occurred attempting to get connector service type [%s] for LMS Connector. PlatformDeployment ID: [%s] not found.",
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
                    "Error occurred attempting to get connector service type [%s] for LMS Connector. PlatformDeployment cannot be null.",
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
            log.error("Error occurred attempting to get connector service type [{}] for LMS Connector: [{}]", type.getSimpleName(), platformDeployment.getLmsConnector(), e);
            throw new TerracottaConnectorException(
                String.format(
                    "Error occurred attempting to get connector service type [%s] for LMS Connector: [%s]",
                    type.getSimpleName(),
                    platformDeployment.getLmsConnector()
                ),
                e
            );
        }
    }

}
