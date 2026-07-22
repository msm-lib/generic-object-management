package com.msm.core.objects.service.imports;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

public class CsvDelimiterDetector {
    private static final char[] CANDIDATES = {
            ',',
            ';',
            '\t',
            '|'
    };

    public static CSVFormat detect(Reader reader, int bufferSize) throws IOException {

        reader.mark(bufferSize);
        String sample = readSample(reader, bufferSize);
        reader.reset();
        char delimiter = detectDelimiter(sample);

        return CSVFormat.DEFAULT.builder()
                .setDelimiter(delimiter)
                .setIgnoreSurroundingSpaces(true)
                .setHeader()
                .setSkipHeaderRecord(true)
                .get();
    }

    private static String readSample(Reader reader, int bufferSize) throws IOException {

        char[] buffer = new char[bufferSize];
        int length = reader.read(buffer);
        if (length <= 0) {
            return "";
        }
        return new String(buffer, 0, length);
    }

    private static char detectDelimiter(String sample) {

        char bestDelimiter = ',';
        int maxColumns = 0;

        for (char delimiter : CANDIDATES) {
            try {
                CSVParser parser = CSVFormat.DEFAULT.builder()
                        .setDelimiter(delimiter)
                        .setHeader()
                        .setSkipHeaderRecord(true)
                        .get()
                        .parse(new StringReader(sample));


                int columns = parser.getHeaderMap().size();
                parser.close();
                if (columns > maxColumns) {
                    maxColumns = columns;
                    bestDelimiter = delimiter;
                }

            } catch (Exception ignored) {
                // ignore invalid delimiter
            }
        }

        return bestDelimiter;
    }
}
