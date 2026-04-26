package com.msm.core.objects.exception;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.msm.core.commons.Utils;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public abstract class BaseErrorDetail {
    protected static final String FULL_ERROR_CODE = "{0}{1}{2}{3}";
    /**
     * SYSTEM_IDENTIFIER is divided by Product Type. - DigiO - 01 - DigiFact - 02 - DigiRetail - 03
     */
    private static final String SYSTEM_IDENTIFIER = "03";

    /**
     * SERVICE_IDENTIFIER is divided by Service - User: 01 - Customer: 02 - Order: 03 - Inventory: 04 - Loyalty: 05 - MasterData: 06
     * - etc
     */
    public static final String SERVICE_IDENTIFIER = "05";

    @JsonIgnore
    protected String tenantId;
    @JsonIgnore
    protected String serviceIdentifier;
    @JsonIgnore
    protected String systemIdentifier;

    public String getTenantId() {
        return Utils.O.defaultIfNull(tenantId, () -> "");
    }

    public String getSystemIdentifier() {
        return Utils.O.defaultIfNull(systemIdentifier, () -> SYSTEM_IDENTIFIER);
    }

    public String getServiceIdentifier() {
        return Utils.O.defaultIfNull(serviceIdentifier, () -> SERVICE_IDENTIFIER);
    }
}
