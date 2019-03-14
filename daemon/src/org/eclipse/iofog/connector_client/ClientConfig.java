/*
 * *******************************************************************************
 *  * Copyright (c) 2019 Edgeworx, Inc.
 *  *
 *  * This program and the accompanying materials are made available under the
 *  * terms of the Eclipse Public License v. 2.0 which is available at
 *  * http://www.eclipse.org/legal/epl-2.0
 *  *
 *  * SPDX-License-Identifier: EPL-2.0
 *  *******************************************************************************
 *
 */
package org.eclipse.iofog.connector_client;

/**
 * Client config to connect to IoFog Connector
 */
public class ClientConfig {
    private int connectorId;
    //same as source microservice uuid
    private String publisherId;
    private String passKey;

    public ClientConfig(int connectorId, String publisherId, String passKey) {
        this.connectorId = connectorId;
        this.publisherId = publisherId;
        this.passKey = passKey;
    }

    public int getConnectorId() {
        return connectorId;
    }

    public String getPublisherId() {
        return publisherId;
    }

    String getPassKey() {
        return passKey;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ClientConfig that = (ClientConfig) o;

        if (connectorId != that.connectorId) return false;
        if (!publisherId.equals(that.publisherId)) return false;
        return passKey.equals(that.passKey);
    }

    @Override
    public int hashCode() {
        int result = connectorId;
        result = 31 * result + publisherId.hashCode();
        result = 31 * result + passKey.hashCode();
        return result;
    }
}
