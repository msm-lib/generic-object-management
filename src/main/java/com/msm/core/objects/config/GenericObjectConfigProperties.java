package com.msm.core.objects.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

@Data
@ConfigurationProperties(prefix = "objects")
public class GenericObjectConfigProperties {
    private Executor executor = new Executor();
    private ImportFile importFile = new ImportFile();

    @Data
    public static class Executor {
        private int core = 1;
        private int max = 20;
    }

    //dev, qc, uat
    @Data
    public static class ImportFile {
        private String basePathUrl;
        private int bufferSize = 64 * 1024;
        private int batchSize = 20;
        private Header header = new Header();
        private Map<String, ObjectConfig> objectConfig = new HashMap<>();

        public int bufferSize(String objectName) {
            if(objectConfig.containsKey(objectName))
                return objectConfig.get(objectName).getBufferSize();

            return bufferSize;
        }

        public int batchSize(String objectName) {
            if(objectConfig.containsKey(objectName))
                return objectConfig.get(objectName).getBatchSize();

            return batchSize;
        }

        public Header header(String objectName) {
            if(objectConfig.containsKey(objectName))
                return objectConfig.get(objectName).getHeader();

            return header;
        }

        public boolean isAutoDetectHeader(String objectName) {
            return header(objectName).mode == Mode.AUTO;
        }

    }

    @Data
    public static class ObjectConfig {
        private int bufferSize = 64 * 1024;
        private int batchSize = 20;
        private Header header;
    }

    @Data
    public static class Header {
        private Mode mode = Mode.AUTO;
        private int row = 0;
        public boolean isAutoDetectHeader() {
            return mode == Mode.AUTO;
        }
    }

    public enum Mode {
        AUTO,
        FIXED
    }
}
