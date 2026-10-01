package com.msm.core.objects.dataexchange.imports.excel;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Locale;

public class ExcelFileValidator {

    private static final long MAX_FILE_SIZE = 50L * 1024 * 1024; // 50 MB

    private static final String XLSX_CONTENT_TYPE =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    public void validate(URL url) throws IOException {
        URLConnection connection = url.openConnection();

        // Không nên download vô hạn
        connection.setConnectTimeout(10_000);
        connection.setReadTimeout(60_000);

        long contentLength = connection.getContentLengthLong();

        if (contentLength > MAX_FILE_SIZE) {
            throw new IllegalArgumentException(
                    "Excel file is too large. Maximum size is 50MB"
            );
        }

        String contentType = connection.getContentType();

        if (contentType != null
                && !contentType.toLowerCase(Locale.ROOT)
                .startsWith(XLSX_CONTENT_TYPE)) {

            throw new IllegalArgumentException(
                    "Invalid Excel content type: " + contentType
            );
        }

        validateExtension(url);
    }

    private void validateExtension(URL url) {
        String path = url.getPath();

        if (path == null || !path.toLowerCase(Locale.ROOT).endsWith(".xlsx")) {
            throw new IllegalArgumentException(
                    "Only .xlsx files are supported"
            );
        }
    }


}
