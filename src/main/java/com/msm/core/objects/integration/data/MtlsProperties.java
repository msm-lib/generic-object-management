package com.msm.core.objects.integration.data;

import lombok.Data;

@Data
public class MtlsProperties {

    private String keyStore;

    private String keyStorePassword;

    private String trustStore;

    private String trustStorePassword;

    private String keyStoreType = "PKCS12";

    private String trustStoreType = "JKS";
}