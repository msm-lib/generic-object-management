package com.msm.core.objects.integration.data;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class BasicCredentials {

    private String username;

    private String password;
}
