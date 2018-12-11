package org.eclipse.iofog.connector_client;

import org.apache.activemq.artemis.api.core.ActiveMQException;
import org.apache.activemq.artemis.api.core.client.ClientConsumer;
import org.apache.activemq.artemis.api.core.client.ClientProducer;

import java.util.HashMap;
import java.util.Map;

import static org.eclipse.iofog.utils.logging.LoggingService.logWarning;

public enum ConnectorManager {
    INSTANCE;

    public final static String MODULE_NAME = "Connector Manager";

    private Map<String, ConnectorProducer> connectorProducers = new HashMap<>();
    private Map<String, ConnectorConsumer> connectorConsumers = new HashMap<>();
    private Map<Integer, ConnectorClient> connectorClients = new HashMap<>();

    public synchronized Map<String, ConnectorProducer> getConnectorProducers() {
        return connectorProducers;
    }

    public synchronized Map<String, ConnectorConsumer> getConnectorConsumers() {
        return connectorConsumers;
    }

    public synchronized void setConnectorClients(Map<Integer, ConnectorClient> connectorClients) {
        for (Integer connectorId : connectorClients.keySet()) {
            if (this.connectorClients.containsKey(connectorId)) {
                ConnectorClient oldConnectorClient = this.connectorClients.get(connectorId);
                ConnectorClient newConnectorClient = connectorClients.get(connectorId);
                if (!oldConnectorClient.getConnectorConfig().equals(newConnectorClient.getConnectorConfig())) {
                    closeConnectorClient(connectorId, oldConnectorClient);
                    this.connectorClients.put(connectorId, newConnectorClient);
                }
            } else {
                this.connectorClients.put(connectorId, connectorClients.get(connectorId));
            }
        }
        this.connectorClients.entrySet().stream()
            .filter(entry -> !connectorClients.containsKey(entry.getKey()))
            .forEach(entry -> closeConnectorClient(entry.getKey(), entry.getValue()));
        this.connectorClients.entrySet().removeIf(entry -> !connectorClients.containsKey(entry.getKey()));
    }

    public synchronized ConnectorProducer getConnectorProducer(String name, ConnectorClientConfig connectorProducerConfig) {
        ConnectorProducer connectorProducer = null;
        if (connectorProducers.containsKey(name) && !connectorProducers.get(name).isClosed()) {
            connectorProducer = connectorProducers.get(name);
        } else if (connectorClients.containsKey(connectorProducerConfig.getConnectorId())) {
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
        } else if (connectorClients.containsKey(connectorConsumerConfig.getConnectorId())) {
            connectorConsumer = createConnectorConsumer(name, connectorConsumerConfig);
            connectorConsumers.put(name, connectorConsumer);
        } else {
            logWarning(MODULE_NAME, String.format("Connector with id %d doesn't exist", connectorConsumerConfig.getConnectorId()));
        }
        return connectorConsumer;
    }

    private void closeConnectorClient(Integer connectorId, ConnectorClient connectorClient) {
        connectorProducers.values().stream()
            .filter(connectorProducer -> connectorProducer.getConfig().getConnectorId() == connectorId)
            .forEach(ConnectorProducer::closeProducer);

        connectorConsumers.values().stream()
            .filter(connectorConsumer -> connectorConsumer.getConfig().getConnectorId() == connectorId)
            .forEach(ConnectorConsumer::closeConsumer);

        connectorClient.closeSession();
    }

    private ConnectorProducer createConnectorProducer(String name, ConnectorClientConfig connectorProducerConfig) {
        ConnectorClient connectorClient = connectorClients.get(connectorProducerConfig.getConnectorId());
        if (connectorClient.getSession() == null) {
            createConnectorSession(connectorClient);
        } else if (connectorClient.getSession().isClosed()) {
            connectorClient.startSession();
        }
        ClientProducer clientProducer = null;
        if (!connectorClient.isClosed()) {
            try {
                clientProducer = connectorClient.createProducer(connectorProducerConfig.getTopicName());
                connectorClient.startSession();
            } catch (ActiveMQException e) {
                logWarning(MODULE_NAME, "Unable to create connector producer: " + e.getMessage());
            }
        }
        return new ConnectorProducer(name, connectorClient, clientProducer, connectorProducerConfig);
    }

    private ConnectorConsumer createConnectorConsumer(String name, ConnectorClientConfig connectorConsumerConfig) {
        ConnectorClient connectorClient = connectorClients.get(connectorConsumerConfig.getConnectorId());
        if (connectorClient.getSession() == null) {
            createConnectorSession(connectorClient);
        } else if (connectorClient.getSession().isClosed()) {
            connectorClient.startSession();
        }
        ClientConsumer clientConsumer = null;
        if (!connectorClient.isClosed()) {
            try {
                clientConsumer = connectorClient.createConsumer(
                    connectorConsumerConfig.getTopicName(),
                    connectorConsumerConfig.getPassKey()
                );
                connectorClient.startSession();
            } catch (ActiveMQException e) {
                logWarning(MODULE_NAME, "Unable to create connector consumer: " + e.getMessage());
            }
        }
        return new ConnectorConsumer(name, connectorClient, clientConsumer, connectorConsumerConfig);
    }

    private void createConnectorSession(ConnectorClient connectorClient) {
        if (connectorClient.getSession() == null) {
            try {
                connectorClient.createSession();
            } catch (Exception e) {
                logWarning(MODULE_NAME, "Unable to create connector session: " + e.getMessage());
            }
        }
    }
}
