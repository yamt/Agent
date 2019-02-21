package org.eclipse.iofog.connector_client;

import org.apache.activemq.artemis.api.core.ActiveMQException;
import org.apache.activemq.artemis.api.core.client.ClientSession;
import org.eclipse.iofog.IOFogModule;
import org.eclipse.iofog.utils.Constants;
import org.eclipse.iofog.utils.functional.Pair;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public enum ConnectorManager implements IOFogModule {
    INSTANCE;

    private Map<Pair<Integer, String>, ConnectorProducer> connectorProducers = new ConcurrentHashMap<>();
    private Map<String, ConnectorConsumer> connectorConsumers = new ConcurrentHashMap<>();
    private Map<Integer, Client> clients = new ConcurrentHashMap<>();

    public Map<Integer, Client> getClients() {
        return clients;
    }

    public Map<Pair<Integer, String>, ConnectorProducer> getConnectorProducers() {
        return connectorProducers;
    }

    public Map<String, ConnectorConsumer> getConnectorConsumers() {
        return connectorConsumers;
    }

    public void setConnectors(Map<Integer, ConnectorConfig> connectors) {
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

    private void closeConnectorClients(Integer connectorId, Client client) {
        connectorProducers.values().stream()
            .filter(connectorProducer -> connectorProducer.getConfig().getConnectorId() == connectorId)
            .forEach(ConnectorProducer::close);

        connectorConsumers.values().stream()
            .filter(connectorConsumer -> connectorConsumer.getConfig().getConnectorId() == connectorId)
            .forEach(ConnectorConsumer::close);

        try {
            client.close();
        } catch (ActiveMQException e) {
            logWarning(String.format("Connector id %d, client close error: %s", connectorId, e.getMessage()));
        }
    }

    public ConnectorProducer getProducer(ClientConfig connectorProducerConfig) {
        ConnectorProducer connectorProducer = null;
        Pair<Integer, String> connectorProducerIdentifiers = Pair.of(
            connectorProducerConfig.getConnectorId(), connectorProducerConfig.getPublisherId()
        );

        if (connectorProducers.containsKey(connectorProducerIdentifiers)) {
            connectorProducer = connectorProducers.get(connectorProducerIdentifiers);
        } else if (clients.containsKey(connectorProducerIdentifiers._1())) {
            initConnectorClient(connectorProducerIdentifiers._1());
            connectorProducer = createProducer(connectorProducerConfig);
            connectorProducers.put(connectorProducerIdentifiers, connectorProducer);
        }
        return connectorProducer;
    }

    public ConnectorConsumer getConsumer(String producerName, ClientConfig consumerConfig) {
        ConnectorConsumer connectorConsumer = null;
        int connectorId = consumerConfig.getConnectorId();

        if (connectorConsumers.containsKey(producerName)) {
            connectorConsumer = connectorConsumers.get(producerName);
        } else if (clients.containsKey(connectorId)) {
            initConnectorClient(connectorId);
            connectorConsumer = createConsumer(producerName, consumerConfig);
            connectorConsumers.put(producerName, connectorConsumer);
        }
        return connectorConsumer;
    }

    public void removeProducer(String name) {
        if (connectorProducers.containsKey(name)) {
            ConnectorProducer producer = connectorProducers.get(name);
            producer.close();
            ejectSession(name, producer.getConfig().getConnectorId());
            connectorProducers.remove(name);
        }
    }

    public void removeConsumer(String name) {
        if (connectorConsumers.containsKey(name)) {
            ConnectorConsumer consumer = connectorConsumers.get(name);
            consumer.close();
            ejectSession(name, consumer.getConfig().getConnectorId());
            connectorConsumers.remove(name);
        }
    }

    private void initConnectorClient(int connectorId) {
        Client client = clients.get(connectorId);
        if (client.getCsf() == null || client.getCsf().isClosed()) {
            try {
                client.init(client.getConfig());
            } catch (Exception e) {
                logWarning(String.format("Connector id %d, session factory creation error: %s", connectorId, e.getMessage()));
            }
        }
    }

    private void ejectSession(String name, int connectorId) {
        if (clients.containsKey(connectorId)) {
            Client client = clients.get(connectorId);
            try {
                client.ejectSession(name);
            } catch (ActiveMQException e) {
                logWarning(String.format(
                    "Connector id %d, session ejection error: %s",
                    connectorId,
                    e.getMessage()));
            }
        }
    }

    private ConnectorProducer createProducer(ClientConfig producerConfig) {
        ClientSession session = null;
        try {
            session = clients.get(producerConfig.getConnectorId()).startSession(producerConfig.getPublisherId());
        } catch (Exception e) {
            logWarning(String.format(
                "Connector id %d, session creation error: %s", producerConfig.getConnectorId(), e.getMessage())
            );
        }
        return new ConnectorProducer(producerConfig.getPublisherId(), session, producerConfig);
    }

    private ConnectorConsumer createConsumer(String name, ClientConfig connectorConsumerConfig) {
        ClientSession session = null;
        try {
            session = clients.get(connectorConsumerConfig.getConnectorId()).startSession(name);
        } catch (Exception e) {
            logWarning(String.format(
                "Connector id %d, session creation error: %s", connectorConsumerConfig.getConnectorId(), e.getMessage())
            );
        }
        return new ConnectorConsumer(name, session, connectorConsumerConfig);
    }

    @Override
    public void start() throws Exception {
        new Thread(new ClientMonitor(this)).start();
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
