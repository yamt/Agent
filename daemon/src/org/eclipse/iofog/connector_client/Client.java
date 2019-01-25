package org.eclipse.iofog.connector_client;

import org.apache.activemq.artemis.api.core.ActiveMQException;
import org.apache.activemq.artemis.api.core.client.ClientSession;
import org.apache.activemq.artemis.api.core.client.ClientSessionFactory;
import org.eclipse.iofog.utils.Constants;

import java.util.ArrayList;
import java.util.List;

import static org.eclipse.iofog.utils.logging.LoggingService.logWarning;

public class Client {

    public static final String MODULE_NAME = "Connector Client";
    private ClientSessionFactory csf;
    private List<ClientSession> sessions = new ArrayList<>();
    private ConnectorConfig config;

    Client(ConnectorConfig config) {
        this.config = config;
        try {
            init(config);
        } catch (Exception e) {
            logWarning(MODULE_NAME, "Connector session factory creation error: " + e.getMessage());
        }
    }

    public synchronized ConnectorConfig getConfig() {
        return config;
    }

    synchronized ClientSessionFactory getCsf() {
        return csf;
    }

    synchronized ClientSession startSession()
        throws ActiveMQException {
        boolean created = false;
        ClientSession session = null;
        try {
            if (csf != null) {
                session = ClientSessions.defaultAuthenticatedSession(csf, config.getUser(), config.getPassword());
                session.start();
                created = true;
                sessions.add(session);
            }
            return session;
        } finally {
            if (!created) {
                close();
            }
        }
    }

    public synchronized void close() throws ActiveMQException {
        if (csf != null && !csf.isClosed()) {
            csf.close();  // closes the sessions too.
        }
        for (ClientSession session : sessions) {
            session.close();
        }
        sessions.clear();
    }

    void init(ConnectorConfig config) throws Exception {
        try {
            close();
        } catch (ActiveMQException e) {
            logWarning(MODULE_NAME, String.format("Connector %s, client close error: %s", config.getName(), e.getMessage()));
        }
        csf = getSessionFactory(config);
    }

    private ClientSessionFactory getSessionFactory(ConnectorConfig config) throws Exception {
        ClientSessionFactory clientSessionFactory;
        if (config.isDevModeEnabled()) {
            clientSessionFactory = ClientSessions.createSessionFactory(config.getHost(), config.getPort());
        } else {
            String truststoreFileName = String.format("%s%s.jks", Constants.TRUSTSTORE_DIR, config.getName());
            ConnectorTruststore.createIfRequired(config.getCert(), truststoreFileName, config.getKeystorePassword());
            clientSessionFactory = ClientSessions.createSessionFactory(
                config.getHost(),
                config.getPort(),
                truststoreFileName,
                config.getKeystorePassword());
        }
        return clientSessionFactory;
    }
}
