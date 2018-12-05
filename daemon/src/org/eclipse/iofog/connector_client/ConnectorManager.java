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

    private void closeConnectorClient(Integer connectorId, ConnectorClient connectorClient) {
        connectorProducers.values().stream()
            .filter(connectorProducer -> connectorProducer.getConfig().getConnectorId().equals(connectorId))
            .forEach(ConnectorProducer::closeProducer);

        connectorConsumers.values().stream()
            .filter(connectorConsumer -> connectorConsumer.getConfig().getConnectorId().equals(connectorId))
            .forEach(ConnectorConsumer::closeConsumer);

        connectorClient.closeSession();
    }

    public synchronized ConnectorProducer getConnectorProducer(String name, ConnectorProducerConfig connectorProducerConfig) {
        ConnectorProducer connectorProducer = null;
        if (connectorProducers.containsKey(name)) {
            connectorProducer = connectorProducers.get(name);
        } else if (connectorClients.containsKey(connectorProducerConfig.getConnectorId())) {
            ConnectorClient connectorClient = connectorClients.get(connectorProducerConfig.getConnectorId());
            createConnectorSession(connectorClient);
            ClientProducer clientProducer = null;
            try {
                clientProducer = connectorClient.createProducer();
                connectorClient.startSession();
            } catch (ActiveMQException e) {
                logWarning(MODULE_NAME, "Unable to create connector producer: " + e.getMessage());
            }
            connectorProducer = new ConnectorProducer(name, connectorClient, clientProducer, connectorProducerConfig);
            connectorProducers.put(name, connectorProducer);
        }
        return connectorProducer;
    }

    public synchronized ConnectorConsumer getConnectorConsumer(String name, ConnectorConsumerConfig connectorConsumerConfig) {
        ConnectorConsumer connectorConsumer = null;
        if (connectorConsumers.containsKey(name)) {
            connectorConsumer = connectorConsumers.get(name);
        } else if (connectorClients.containsKey(connectorConsumerConfig.getConnectorId())) {
            ConnectorClient connectorClient = connectorClients.get(connectorConsumerConfig.getConnectorId());
            if (connectorClient.getSession() == null) {
                createConnectorSession(connectorClient);
            }
            ClientConsumer clientConsumer = null;

            try {
                clientConsumer = connectorClient.createConsumer(
                    connectorConsumerConfig.getTopicName(),
                    String.format("key='%s'", connectorConsumerConfig.getPassKey())
                );
                connectorClient.startSession();
            } catch (ActiveMQException e) {
                logWarning(MODULE_NAME, "Unable to create connector consumer: " + e.getMessage());
            }
            connectorConsumer = new ConnectorConsumer(name, connectorClient, clientConsumer, connectorConsumerConfig);
            connectorConsumers.put(name, connectorConsumer);
        }
        return connectorConsumer;
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

//    public CompletableFuture<Void> createConnectorProducer(String name, Integer connectorId) {
//        CompletableFuture<Void> completableFuture = CompletableFuture.completedFuture(null);
//        if (!connectorProducers.containsKey(name)) {
//            ConnectorClient connectorClient = connectorClients.get(connectorId);
//            completableFuture = createConnectorSession(connectorId)
//                .thenRunAsync(createConnectorProducerRunnable(name, connectorClient))
//                .thenRunAsync(startSession(connectorClient));
//        }
//        return completableFuture;
//    }

//    public CompletableFuture<Void> createConnectorConsumer(String name, Integer connectorId, String topicName, String filter) {
//        CompletableFuture<Void> completableFuture = CompletableFuture.completedFuture(null);
//        if (!connectorConsumers.containsKey(name)) {
//            ConnectorClient connectorClient = connectorClients.get(connectorId);
//            completableFuture = createConnectorSession(connectorId)
//                .thenRunAsync(createConnectorConsumerRunnable(name, connectorClient, topicName, filter))
//                .thenRunAsync(startSession(connectorClient));
//        }
//        return completableFuture;
//    }
//
//    private CompletableFuture<Void> createConnectorSession(Integer connectorId) {
//        CompletableFuture<Void> completableFuture = CompletableFuture.completedFuture(null);
//        if (connectorClients.containsKey(connectorId)
//            && connectorClients.get(connectorId).getSession() == null) {
//            completableFuture = CompletableFuture.runAsync(createConnectorSessionRunnable(connectorClients.get(connectorId)));
//        }
//        return completableFuture;
//    }
//
//    private Runnable startSession(ConnectorClient connectorClient) {
//        return () -> {
//            try {
//                connectorClient.startSession();
//            } catch (ActiveMQException e) {
//                logWarning(MODULE_NAME, "Unable to start connector session");
//            }
//        };
//    }

//    private Runnable createConnectorProducerRunnable(String name, ConnectorClient connectorClient) {
//        return () -> {
//            boolean isProducerCreated = false;
//            while(!isProducerCreated) {
//                if (Thread.currentThread().isInterrupted()) {
//                    break;
//                }
//                try {
//                    ClientProducer producer = connectorClient.createProducer();
//                    ConnectorProducer connectorProducer = new ConnectorProducer(name, connectorClient, producer);
//                    connectorProducers.put(name, connectorProducer);
//                    isProducerCreated = true;
//                    logInfo(MODULE_NAME, "Connector producer has been created.");
//                } catch (ActiveMQException e) {
//                    logWarning(MODULE_NAME, "Failed to create connector producer: " + e.getMessage());
//                    logInfo(MODULE_NAME, "Going to create connector producer in 10 seconds.");
//                    try {
//                        Thread.sleep(10000);
//                    } catch (InterruptedException ex) {
//                        logInfo(MODULE_NAME, ex.getMessage());
//                    }
//                }
//            }
//        };
//    }

//    private Runnable createConnectorConsumerRunnable(String name, ConnectorClient connectorClient, String topicName, String filter) {
//        return () -> {
//            boolean isConsumerCreated = false;
//            while(!isConsumerCreated) {
//                if (Thread.currentThread().isInterrupted()) {
//                    break;
//                }
//                try {
//                    ClientConsumer consumer = connectorClient.createConsumer(topicName, filter);
//                    ConnectorConsumer connectorConsumer = new ConnectorConsumer(name, connectorClient, consumer);
//                    connectorConsumers.put(name, connectorConsumer);
//                    isConsumerCreated = true;
//                    logInfo(MODULE_NAME, "Connector consumer has been created.");
//                } catch (ActiveMQException e) {
//                    logWarning(MODULE_NAME, "Failed to create connector consumer: " + e.getMessage());
//                    logInfo(MODULE_NAME, "Going to re-create connector consumer in 10 seconds.");
//                    try {
//                        Thread.sleep(10000);
//                    } catch (InterruptedException ex) {
//                        logInfo(MODULE_NAME, ex.getMessage());
//                    }
//                }
//            }
//        };
//    }

//    private Runnable createConnectorSessionRunnable(ConnectorClient connector) {
//        return () -> {
//            ClientSession session = null;
//            boolean isConnectorSessionCreated = false;
//            while(!isConnectorSessionCreated) {
//                if (Thread.currentThread().isInterrupted()) {
//                    break;
//                }
//                try {
//                    connector.createSession();
//                    isConnectorSessionCreated = true;
//                } catch (Exception e) {
//                    logWarning(MODULE_NAME, "Unable to create connector session: " + e.getMessage());
//                    logInfo(MODULE_NAME, "Going to re-create connector session in 10 seconds.");
//                    try {
//                        Thread.sleep(10000);
//                    } catch (InterruptedException ex) {
//                        logInfo(MODULE_NAME, ex.getMessage());
//                    }
//                }
//            }
//        };
//    }
}
