package com.msm.core.objects.integration.factory;

import com.msm.core.objects.integration.data.MtlsProperties;
import org.springframework.core.io.ClassPathResource;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.SecureRandom;

public class SslContextFactory {

    public static SSLContext create(MtlsProperties props) {

        try {

            KeyStore keyStore = KeyStore.getInstance("PKCS12");

            try (InputStream is = new ClassPathResource(props.getKeyStore()).getInputStream()) {
                keyStore.load(is, props.getKeyStorePassword().toCharArray());
            }

            KeyStore trustStore = KeyStore.getInstance("JKS");

            try (InputStream is = new ClassPathResource(props.getTrustStore()).getInputStream()) {
                trustStore.load(is, props.getTrustStorePassword().toCharArray());
            }

            KeyManagerFactory kmf =
                    KeyManagerFactory.getInstance(
                            KeyManagerFactory.getDefaultAlgorithm()
                    );

            kmf.init(
                    keyStore,
                    props.getKeyStorePassword().toCharArray()
            );

            TrustManagerFactory tmf =
                    TrustManagerFactory.getInstance(
                            TrustManagerFactory.getDefaultAlgorithm()
                    );

            tmf.init(trustStore);

            SSLContext sslContext =
                    SSLContext.getInstance("TLS");

            sslContext.init(
                    kmf.getKeyManagers(),
                    tmf.getTrustManagers(),
                    new SecureRandom()
            );

            return sslContext;

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}