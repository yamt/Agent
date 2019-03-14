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

import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableEntryException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;

import static org.eclipse.iofog.utils.logging.LoggingService.logInfo;

/**
 * Server cert truststore to communicate to IoFog Connector over TLS
 */
class ConnectorTruststore {
    public static final String MODULE_NAME = "Connector Truststore";

    static void createIfRequired(String certContent, String truststoreFileName, String truststorePassword)
        throws KeyStoreException, UnrecoverableEntryException, CertificateException, NoSuchAlgorithmException, IOException {
        boolean needToCreate;

        if (Files.exists(Paths.get(truststoreFileName))) {
            InputStream inputStream = new ByteArrayInputStream(certContent.getBytes());
            Certificate cert = getCert(inputStream);

            KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
            char[] password = truststorePassword.toCharArray();

            try {
                ks.load(Files.newInputStream(Paths.get(truststoreFileName)), password);
                Certificate oldCert = ((KeyStore.TrustedCertificateEntry) ks.getEntry(truststoreFileName, null)).getTrustedCertificate();
                needToCreate = !cert.equals(oldCert);
            } catch (IOException | CertificateException | NoSuchAlgorithmException e) {
                logInfo(MODULE_NAME, e.getMessage());
                needToCreate = true;
            }
        } else {
            needToCreate = true;
        }

        if (needToCreate) {
            create(certContent, truststoreFileName, truststorePassword);
        }

    }

    private static void create(String certContent, String truststoreFileName, String truststorePassword)
        throws KeyStoreException, CertificateException, NoSuchAlgorithmException, IOException {
        try (InputStream inputStream = new ByteArrayInputStream(certContent.getBytes());
             FileOutputStream fos = new FileOutputStream(truststoreFileName)) {
            Certificate cert = getCert(inputStream);

            KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
            char[] password = truststorePassword.toCharArray();

            ks.load(null, password);
            KeyStore.TrustedCertificateEntry trustedCertificateEntry = new KeyStore.TrustedCertificateEntry(cert);
            ks.setEntry(truststoreFileName, trustedCertificateEntry, null);
            ks.store(fos, password);
        }
    }

    private static Certificate getCert(InputStream is) {
        Certificate result = null;
        try {
            CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
            result = certificateFactory.generateCertificate(is);
        } catch (CertificateException exp) {
            exp.printStackTrace();
        }
        return result;
    }
}
