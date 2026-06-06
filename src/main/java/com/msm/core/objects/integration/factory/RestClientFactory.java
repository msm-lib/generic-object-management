package com.msm.core.objects.integration.factory;

import com.msm.core.objects.integration.data.ConnectorProperties;
import org.springframework.web.client.RestClient;

public class RestClientFactory {

    public static RestClient create(
            ConnectorProperties props) {

        RestClient.Builder builder =
                RestClient.builder()
                        .baseUrl(props.getBaseUrl());
//
//        // CASE 1: mTLS
//        if (props.getAuth().getMtls() != null) {
//
//            SSLContext sslContext =
//                    SslContextFactory.create(
//                            props.getAuth().getMtls()
//                    );
//
//            HttpComponentsClientHttpRequestFactory factory =
//                    new HttpComponentsClientHttpRequestFactory();
//
//            //factory.setSslContext(sslContext);
//
//            builder.requestFactory(factory);
//        }

//        if (props.getTimeoutMs() != null) {
//
//            HttpComponentsClientHttpRequestFactory factory =
//                    new HttpComponentsClientHttpRequestFactory();
//
//            factory.setConnectTimeout(
//                    Duration.ofMillis(props.getTimeoutMs())
//            );
//
//            builder.requestFactory(factory);
//        }

        return builder.build();
    }
}
