package org.eclipse.iofog.connector_client;

import org.apache.activemq.artemis.api.core.ActiveMQException;
import org.apache.activemq.artemis.api.core.client.ClientSession;
import org.eclipse.iofog.IOFogModule;
import org.eclipse.iofog.utils.Constants;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public enum ConnectorManager implements IOFogModule {
    INSTANCE;

    private Map<String, ConnectorProducer> connectorProducers = new ConcurrentHashMap<>();
    private Map<String, ConnectorConsumer> connectorConsumers = new ConcurrentHashMap<>();
    private Map<Integer, Client> clients = new ConcurrentHashMap<>();

    public synchronized Map<String, ConnectorProducer> getConnectorProducers() {
        return connectorProducers;
    }

    public synchronized Map<String, ConnectorConsumer> getConnectorConsumers() {
        return connectorConsumers;
    }

    public synchronized void setConnectors(Map<Integer, ConnectorConfig> connectors) {
        connectors.forEach(this::updateConnector);

        this.clients.entrySet().stream()
            .filter(entry -> !connectors.containsKey(entry.getKey()))
            .forEach(entry -> closeConnectorClients(entry.getKey(), entry.getValue()));
        this.clients.entrySet().removeIf(entry -> !connectors.containsKey(entry.getKey()));
    }

    private void updateConnector(int id, ConnectorConfig config) {
        if (clients.containsKey(id)) {
            Client client = this.clients.get(id);
            if (!client.getConfig().equals(config)) {
                try {
                    client.close();
                } catch (ActiveMQException e) {
                    logWarning("Connector client close error: " + e.getMessage());
                }
                clients.put(id, new Client(config));
            }
        } else {
            clients.put(id, new Client(config));
        }
    }

    private Runnable clientMonitor() {
        return () -> {
            while (true) {
                try {
                    Thread.sleep(10000);

                    for (Map.Entry<Integer, Client> clientEntry : clients.entrySet()) {
                        Client client = clientEntry.getValue();

                        try {
                            if (client.getCsf() == null || client.getCsf().isClosed()) {
                                client.init(client.getConfig());
                            }
                            connectorConsumers.values().stream()
                                .filter(connectorConsumer ->
                                    connectorConsumer.getConfig().getConnectorId() == clientEntry.getKey())
                                .forEach(connectorConsumer -> {
                                    try {
                                        if (connectorConsumer.isClosed()) {
                                            ClientSession session = client.startSession();
                                            connectorConsumer.init(session, connectorConsumer.getConfig());
                                        }
                                    } catch (ActiveMQException e) {
                                        logWarning(String.format(
                                            "Connector id %d, session creation error: %s", clientEntry.getKey(), e.getMessage())
                                        );
                                    }
                                });
                            connectorProducers.values().stream()
                                .filter(connectorProducer ->
                                    connectorProducer.getConfig().getConnectorId() == clientEntry.getKey())
                                .forEach(connectorProducer -> {
                                    try {
                                        if (connectorProducer.isClosed()) {
                                            ClientSession session = client.startSession();
                                            connectorProducer.init(session);
                                        }
                                    } catch (ActiveMQException e) {
                                        logWarning(String.format(
                                            "Connector id %d, session creation error: %s", clientEntry.getKey(), e.getMessage())
                                        );
                                    }
                                });
                        } catch (Exception e) {
                            logWarning(
                                String.format("Connector id %d, session factory creation error: %s", clientEntry.getKey(), e.getMessage())
                            );
                        }

                    }
                } catch (InterruptedException ex) {
                    logWarning(ex.getMessage());
                }
            }
        };
    }

    public synchronized ConnectorProducer getProducer(String name, ClientConfig connectorProducerConfig) {
        ConnectorProducer connectorProducer = null;
        if (connectorProducers.containsKey(name)) {
            connectorProducer = connectorProducers.get(name);
        } else if (clients.containsKey(connectorProducerConfig.getConnectorId())) {
            connectorProducer = createProducer(name, connectorProducerConfig);
            connectorProducers.put(name, connectorProducer);
        } else {
            logWarning(String.format("Connector with id %d doesn't exist", connectorProducerConfig.getConnectorId()));
        }
        return connectorProducer;
    }

    public synchronized ConnectorConsumer getConsumer(String name, ClientConfig consumerConfig) {
        ConnectorConsumer connectorConsumer = null;
        if (connectorConsumers.containsKey(name)) {
            connectorConsumer = connectorConsumers.get(name);
        } else if (clients.containsKey(consumerConfig.getConnectorId())) {
            connectorConsumer = createConsumer(name, consumerConfig);
            connectorConsumers.put(name, connectorConsumer);
        } else {
            logWarning(String.format("Connector with id %d doesn't exist", consumerConfig.getConnectorId()));
        }
        return connectorConsumer;
    }

    private ConnectorProducer createProducer(String name, ClientConfig producerConfig) {
        ClientSession session = null;
        try {
            session = clients.get(producerConfig.getConnectorId()).startSession();
        } catch (Exception e) {
            logWarning(String.format(
                "Connector id %d, session creation error: %s", producerConfig.getConnectorId(), e.getMessage())
            );
        }
        return new ConnectorProducer(name, session, producerConfig);
    }

    private ConnectorConsumer createConsumer(String name, ClientConfig connectorConsumerConfig) {
        ClientSession session = null;
        try {
            session = clients.get(connectorConsumerConfig.getConnectorId()).startSession();
        } catch (Exception e) {
            logWarning(String.format(
                "Connector id %d, session creation error: %s", connectorConsumerConfig.getConnectorId(), e.getMessage())
            );
        }
        return new ConnectorConsumer(name, session, connectorConsumerConfig);
    }

    private void closeConnectorClients(Integer connectorId, Client client) {
        connectorProducers.values().stream()
            .filter(connectorProducer -> connectorProducer.getConfig().getConnectorId() == connectorId)
            .forEach(ConnectorProducer::closeProducer);

        connectorConsumers.values().stream()
            .filter(connectorConsumer -> connectorConsumer.getConfig().getConnectorId() == connectorId)
            .forEach(ConnectorConsumer::closeConsumer);

        try {
            client.close();
        } catch (ActiveMQException e) {
            logWarning(String.format("Connector id %d, client close error: %s", connectorId, e.getMessage()));
        }
    }

    @Override
    public void start() throws Exception {
        new Thread(clientMonitor()).start();
    }

    @Override
    public int getModuleIndex() {
        return Constants.CONNECTOR_MANAGER;
    }

    @Override
    public String getModuleName() {
        return "Connector Manager";
    }
}
