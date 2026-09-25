package com.msm.core.objects;

public final class ObjectActionNamed {
    private ObjectActionNamed(){}

    public static final class Csv {
        public static final String NAME = "csv";
        //Main handler from user
        public static final String VALIDATION = "csv_import_validation_processing";
        public static final String IMPORT_DATA = "csv_import_data_processing";
        public static final String READ_FILE = "csv_read_processing";
        public static final String DETECT_COLUMN_HEADER_MAPPING = "csv_detect_column_mapping_processing";
        public static final String ROW_MAPPING = "csv_row_mapping_processing";
        public static final String CELL_MAPPING = "csv_cell_mapping_processing";
        public static final String FIELD_REFERENCE_RESOLVE = "csv_field_reference_resolve_processing";
        public static final String DATA_VALIDATE_PROCESSING = "csv_batch_row_data_processing";
    }

    public static final class Excel {
        public static final String NAME = "excel";
        //Main handler from user
        public static final String VALIDATION = "excel_import_validation_processing";
        public static final String IMPORT_DATA = "excel_import_data_processing";


        public static final String READ_FILE = "excel_read_processing";
        public static final String DETECT_COLUMN_HEADER_MAPPING = "excel_detect_column_mapping_processing";
        public static final String ROW_MAPPING = "excel_row_mapping_processing";
        public static final String CELL_MAPPING = "excel_cell_mapping_processing";
        public static final String FIELD_REFERENCE_RESOLVE = "excel_field_reference_resolve_processing";
        public static final String DATA_VALIDATE_PROCESSING = "excel_batch_data_validate_processing";

        public static final String DOWNLOAD_FILE_ERRORS = "excel_download_file_error_processing";
        public static final String FILE_PROCESSED_EVENT = "excel_file_processed_event";

        public static final String BATCH_INSERT_OR_UPDATE_DATA_PROCESSING = "excel_batch_insert_or_update_processing";


        public static final class Export {
            public static final String CREATE_HEADER = "export_excel_create_header_processing";
            public static final String DETECT_COLUMN_HEADER_MAPPING = "export_excel_detect_column_mapping_processing";
            public static final String ROW_MAPPING = "export_excel_row_mapping_processing";
            public static final String CELL_MAPPING = "export_excel_cell_mapping_processing";
            public static final String CREATE_CELL = "export_excel_create_cell_processing";
        }

    }

    public static final class IntegrationLog {
        public static final String NAME = "excel";
    }
}
