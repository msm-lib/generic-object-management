package com.msm.core.objects.config;

import com.msm.core.commons.Utils;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Data
@ConfigurationProperties(prefix = "imports")
public class ObjectImportRegistry {

    private String basePathUrl;
    private HeaderConfig header;
    private ProcessingConfig processing;
    private ConversionConfig conversion;
    private Map<String, ObjectConfig> objects = new HashMap<>();

    @Data
    public static class HeaderConfig {
        private Mode mode = Mode.AUTO;
        private int row = 0;
        public boolean isAutoDetectHeader() {
            return mode == Mode.AUTO;
        }
    }

    @Data
    public static class ProcessingConfig {
        private int bufferSize = 64 * 1024;
        private int batchSize = 100;
    }

    @Data
    public static class ObjectConfig {
        private ProcessingConfig processing = new ProcessingConfig();
        private IdentityConfig identity = new IdentityConfig();
        private Map<String, ReferenceDetailConfig> references = new HashMap<>();
    }

    @Data
    public static class IdentityConfig {
        private List<String> fields = Utils.CL.newArrayList("code");
        private StrategyMode strategy = StrategyMode.UPSERT;
        //condition: "is_deleted IS NOT TRUE"
        private String condition;
    }

    @Data
    public static class ReferenceDetailConfig {
        private List<LookupValuesConfig> lookups = Utils.CL.newArrayList(new LookupValuesConfig());
        private List<String> fields = Utils.CL.newArrayList("id", "code", "name");
        private Map<String, String> mappingKeys = new HashMap<>();
    }

    @Data
    public static class LookupValuesConfig {
        private String attributeName = "code";
        private Set<String> defaultValues = null;
        private boolean primary = false;
    }

    @Data
    public static class ConversionConfig {
        private BooleanConfig booleanConfig;
        private DateConfig date;
        private DateTimeConfig datetime;
        private DecimalConfig decimal;
        private NullConfig nullConfig;
    }

    @Data
    public static class BooleanConfig {
        private Set<String> trueValues = new HashSet<>();
        private Set<String> falseValues = new HashSet<>();
    }

    @Data
    public static class DateConfig {
        private List<String> formats = new ArrayList<>();
    }

    @Data
    public static class DateTimeConfig {
        private List<String> formats = new ArrayList<>();
    }

    @Data
    public static class DecimalConfig {
        private String decimalSeparator = ".";
        private String groupingSeparator = ",";
    }

    @Data
    public static class NullConfig {
        private List<String> values = new ArrayList<>();
    }

    public enum Mode {
        AUTO,
        FIXED
    }

    public enum StrategyMode {
        UPSERT,
        LOOKUP,
        NONE
    }
}
