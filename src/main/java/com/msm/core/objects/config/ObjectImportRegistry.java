package com.msm.core.objects.config;

import com.msm.core.commons.Utils;
import com.msm.core.objects.dataexchange.imports.model.ImportMode;
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
        private ProcessingConfig processing;
        private SourceConfig source = new SourceConfig();
        private IdentityConfig identity = new IdentityConfig();
        private Map<String, ReferenceDetailConfig> references = new HashMap<>();
        private List<String> dependsOn = new ArrayList<>();
        /** Import of this object's rows as details of one header (action import_details on the header); null = none. */
        private ParentConfig parent;

        /**
         * @deprecated use {@code source.header}; kept so {@code imports.objects.<obj>.header} still binds.
         */
        @Deprecated
        public void setHeader(HeaderConfig header) {
            source.setHeader(header);
        }
    }

    @Data
    public static class ParentConfig {
        /** Header object. */
        private String object;
        /** Field of this object holding the header id; the import fills it. */
        private String field;
        /** Fields of the header copied into the reference of {@link #field} (its attributeRef) on every row. */
        private List<String> referenceFields = Utils.CL.newArrayList("id", "code", "name");
        /** True: rows of this object are imported only through a header (import_details), never on their own. */
        private boolean required;
        /** Identity of a row among the header's details; {@link #field} is put first. */
        private List<String> identity = new ArrayList<>();
        private List<ImportMode> modes = Utils.CL.newArrayList(ImportMode.APPEND);
        private ImportMode defaultMode = ImportMode.APPEND;
        /** Most rows a file may have; 0 = no limit. */
        private int maxRows;
    }

    @Data
    public static class SourceConfig {
        // Excel sheet name; blank = first sheet
        private String sheet;
        private HeaderConfig header;
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
        private List<LookupValuesConfig> lookups;
        private List<String> fields = Utils.CL.newArrayList("id", "code", "name");
        private Map<String, String> mappingValues = new HashMap<>();
    }

    @Data
    public static class LookupValuesConfig {
        private String attributeName = "code";
        private String sourceField;
        private String filterAttribute;
        private Set<String> defaultValues = null;
        private boolean primary = true;

        public String getFilterAttribute() {
            return Utils.STR.defaultIfBlank(filterAttribute, attributeName);
        }
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
