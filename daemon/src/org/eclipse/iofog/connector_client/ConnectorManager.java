package org.eclipse.iofog.connector_client;

import org.apache.activemq.artemis.api.core.ActiveMQException;
import org.apache.activemq.artemis.api.core.client.ClientSession;

import java.util.HashMap;
import java.util.Map;

import static org.eclipse.iofog.utils.logging.LoggingService.logWarning;

public enum ConnectorManager {
    INSTANCE;

    public final static String MODULE_NAME = "Connector Manager";

    private Map<String, ConnectorProducer> connectorProducers = new HashMap<>();
    private Map<String, ConnectorConsumer> connectorConsumers = new HashMap<>();
    private Map<Integer, ConnectorSessionPool> connectorSessionPoolMap = new HashMap<>();

    public synchronized Map<String, ConnectorProducer> getConnectorProducers() {
        return connectorProducers;
    }

    public synchronized Map<String, ConnectorConsumer> getConnectorConsumers() {
        return connectorConsumers;
    }

    public synchronized void setConnectors(Map<Integer, ConnectorConfig> connectors) {
        connectors.forEach((id, config) -> {
            if (connectorSessionPoolMap.containsKey(id)) {
                ConnectorSessionPool connectorSessionPool = connectorSessionPoolMap.get(id);
                if (!connectorSessionPool.getConnectorConfig().equals(config)) {
                    try {
                        connectorSessionPool.shutdown();
                    } catch (ActiveMQException e) {
                        logWarning(MODULE_NAME, "Connector session pool shutdown error: " + e.getMessage());
                    }
                    addConnectorSessionPool(id, config);
                }
            } else {
                addConnectorSessionPool(id, config);
            }
        });

        this.connectorSessionPoolMap.entrySet().stream()
            .filter(entry -> !connectors.containsKey(entry.getKey()))
            .forEach(entry -> closeConnectorClients(entry.getKey(), entry.getValue()));
        this.connectorSessionPoolMap.entrySet().removeIf(entry -> !connectors.containsKey(entry.getKey()));
    }

    public synchronized ConnectorProducer getConnectorProducer(String name, ConnectorClientConfig connectorProducerConfig) {
        ConnectorProducer connectorProducer = null;
        if (connectorProducers.containsKey(name) && !connectorProducers.get(name).isClosed()) {
            connectorProducer = connectorProducers.get(name);
        } else if (connectorSessionPoolMap.containsKey(connectorProducerConfig.getConnectorId())) {
            connectorProducer = createConnectorProducer(name, connectorProducerConfig);
            connectorProducers.put(name, connectorProducer);
        } else {
            logWarning(MODULE_NAME, String.format("Connector with id %d doesn't exist", connectorProducerConfig.getConnectorId()));
        }
        return connectorProducer;
    }

    public synchronized ConnectorConsumer getConnectorConsumer(String name, ConnectorClientConfig connectorConsumerConfig) {
        ConnectorConsumer connectorConsumer = null;
        if (connectorConsumers.containsKey(name) && !connectorConsumers.get(name).isClosed()) {
            connectorConsumer = connectorConsumers.get(name);
        } else if (connectorSessionPoolMap.containsKey(connectorConsumerConfig.getConnectorId())) {
            connectorConsumer = createConnectorConsumer(name, connectorConsumerConfig);
            connectorConsumers.put(name, connectorConsumer);
        } else {
            logWarning(MODULE_NAME, String.format("Connector with id %d doesn't exist", connectorConsumerConfig.getConnectorId()));
        }
        return connectorConsumer;
    }

    private ConnectorProducer createConnectorProducer(String name, ConnectorClientConfig connectorProducerConfig) {
        ConnectorProducer producer = null;
        try {
            ClientSession session = connectorSessionPoolMap.get(connectorProducerConfig.getConnectorId()).getSession();
            producer = new ConnectorProducer(name, session, connectorProducerConfig);
        } catch (Exception e) {
            logWarning(MODULE_NAME, "Connector producer creation error: " + e.getMessage());
        }
        return producer;
    }

    private ConnectorConsumer createConnectorConsumer(String name, ConnectorClientConfig connectorConsumerConfig) {
        ConnectorConsumer consumer = null;
        try {
            ClientSession session = connectorSessionPoolMap.get(connectorConsumerConfig.getConnectorId()).getSession();
            consumer = new ConnectorConsumer(name, session, connectorConsumerConfig);
        } catch (Exception e) {
            logWarning(MODULE_NAME, "Connector consumer creation error: " + e.getMessage());
        }
        return consumer;
    }

    private void addConnectorSessionPool(int id, ConnectorConfig config) {
        try {
            connectorSessionPoolMap.put(id, ConnectorSessionPool.create(config));
        } catch (Exception e) {
            logWarning(MODULE_NAME, "Connector session pool creation error: " + e.getMessage());
        }
    }

    private void closeConnectorClients(Integer connectorId, ConnectorSessionPool connectorSessionPool) {
        connectorProducers.values().stream()
            .filter(connectorProducer -> connectorProducer.getConfig().getConnectorId() == connectorId)
            .forEach(ConnectorProducer::closeProducer);

        connectorConsumers.values().stream()
            .filter(connectorConsumer -> connectorConsumer.getConfig().getConnectorId() == connectorId)
            .forEach(ConnectorConsumer::closeConsumer);

        try {
            connectorSessionPool.shutdown();
        } catch (ActiveMQException e) {
            logWarning(MODULE_NAME, "Connector session pool shutdown error: " + e.getMessage());
        }
    }
}
