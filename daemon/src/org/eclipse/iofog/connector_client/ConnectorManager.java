package org.eclipse.iofog.connector_client;

import org.apache.activemq.artemis.api.core.ActiveMQException;
import org.apache.activemq.artemis.api.core.client.ClientConsumer;
import org.apache.activemq.artemis.api.core.client.ClientProducer;
import org.apache.activemq.artemis.api.core.client.ClientSession;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.eclipse.iofog.utils.logging.LoggingService.logInfo;
import static org.eclipse.iofog.utils.logging.LoggingService.logWarning;

public enum ConnectorManager {
    INSTANCE;

    public final static String MODULE_NAME = "Connector Manager";

    private Map<String, ConnectorProducer> connectorProducers = new HashMap<>();
    private Map<String, ConnectorConsumer> connectorConsumers = new HashMap<>();
    private Map<Integer, ConnectorClient> connectorClients = new HashMap<>();

    public CompletableFuture<ConnectorProducer> getConnectorProducer(String name, Integer connectorId) {
        CompletableFuture<ConnectorProducer> completableFuture;
        if (connectorProducers.containsKey(name)) {
            completableFuture = CompletableFuture.completedFuture(connectorProducers.get(name));
        } else {
            completableFuture = CompletableFuture.supplyAsync(() -> {
                createConnectorProducer(name, connectorId)
            })

        }
    }

    public CompletableFuture<Void> createConnectorProducer(String name, Integer connectorId) {
        CompletableFuture<Void> completableFuture = CompletableFuture.completedFuture(null);
        if (!connectorProducers.containsKey(name)) {
            ConnectorClient connectorClient = connectorClients.get(connectorId);
            completableFuture = createConnectorSession(connectorId)
                .thenRunAsync(createConnectorProducerRunnable(name, connectorClient))
                .thenRunAsync(startSession(connectorClient));
        }
        return completableFuture;
    }

    public CompletableFuture<Void> createConnectorConsumer(String name, Integer connectorId, String topicName, String filter) {
        CompletableFuture<Void> completableFuture = CompletableFuture.completedFuture(null);
        if (!connectorConsumers.containsKey(name)) {
            ConnectorClient connectorClient = connectorClients.get(connectorId);
            completableFuture = createConnectorSession(connectorId)
                .thenRunAsync(createConnectorConsumerRunnable(name, connectorClient, topicName, filter))
                .thenRunAsync(startSession(connectorClient));
        }
        return completableFuture;
    }

    private CompletableFuture<Void> createConnectorSession(Integer connectorId) {
        CompletableFuture<Void> completableFuture = CompletableFuture.completedFuture(null);
        if (connectorClients.containsKey(connectorId)
            && connectorClients.get(connectorId).getSession() == null) {
            completableFuture = CompletableFuture.runAsync(createConnectorSessionRunnable(connectorClients.get(connectorId)));
        }
        return completableFuture;
    }

    private Runnable startSession(ConnectorClient connectorClient) {
        return () -> {
            try {
                connectorClient.startSession();
            } catch (ActiveMQException e) {
                logWarning(MODULE_NAME, "Unable to start connector session");
            }
        };
    }

    private Runnable createConnectorProducerRunnable(String name, ConnectorClient connectorClient) {
        return () -> {
            boolean isProducerCreated = false;
            while(!isProducerCreated) {
                if (Thread.currentThread().isInterrupted()) {
                    break;
                }
                try {
                    ClientProducer producer = connectorClient.createProducer();
                    ConnectorProducer connectorProducer = new ConnectorProducer(name, connectorClient, producer);
                    connectorProducers.put(name, connectorProducer);
                    isProducerCreated = true;
                    logInfo(MODULE_NAME, "Connector producer has been created.");
                } catch (ActiveMQException e) {
                    logWarning(MODULE_NAME, "Failed to create connector producer: " + e.getMessage());
                    logInfo(MODULE_NAME, "Going to create connector producer in 10 seconds.");
                    try {
                        Thread.sleep(10000);
                    } catch (InterruptedException ex) {
                        logInfo(MODULE_NAME, ex.getMessage());
                    }
                }
            }
        };
    }

    private Runnable createConnectorConsumerRunnable(String name, ConnectorClient connectorClient, String topicName, String filter) {
        return () -> {
            boolean isConsumerCreated = false;
            while(!isConsumerCreated) {
                if (Thread.currentThread().isInterrupted()) {
                    break;
                }
                try {
                    ClientConsumer consumer = connectorClient.createConsumer(topicName, filter);
                    ConnectorConsumer connectorConsumer = new ConnectorConsumer(name, connectorClient, consumer);
                    connectorConsumers.put(name, connectorConsumer);
                    isConsumerCreated = true;
                    logInfo(MODULE_NAME, "Connector consumer has been created.");
                } catch (ActiveMQException e) {
                    logWarning(MODULE_NAME, "Failed to create connector consumer: " + e.getMessage());
                    logInfo(MODULE_NAME, "Going to re-create connector consumer in 10 seconds.");
                    try {
                        Thread.sleep(10000);
                    } catch (InterruptedException ex) {
                        logInfo(MODULE_NAME, ex.getMessage());
                    }
                }
            }
        };
    }

    private Runnable createConnectorSessionRunnable(ConnectorClient connector) {
        return () -> {
            ClientSession session = null;
            boolean isConnectorSessionCreated = false;
            while(!isConnectorSessionCreated) {
                if (Thread.currentThread().isInterrupted()) {
                    break;
                }
                try {
                    connector.createSession();
                    isConnectorSessionCreated = true;
                } catch (Exception e) {
                    logWarning(MODULE_NAME, "Unable to create connector session: " + e.getMessage());
                    logInfo(MODULE_NAME, "Going to re-create connector session in 10 seconds.");
                    try {
                        Thread.sleep(10000);
                    } catch (InterruptedException ex) {
                        logInfo(MODULE_NAME, ex.getMessage());
                    }
                }
            }
        };
    }
}
