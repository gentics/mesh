package com.gentics.mesh.plugin.export.model;

import com.opencsv.CSVWriter;

import java.io.StringWriter;
import java.util.List;

public class CsvStringWriter {

    private StringWriter stringWriter;
    private CSVWriter csvWriter;

    public CsvStringWriter() {
        stringWriter = new StringWriter();
        csvWriter = new CSVWriter(stringWriter, ';');
    }

    public void writeRow(List<String> row) {
        if (row != null) {
            csvWriter.writeNext(row.toArray(new String[]{}));
        }
    }

    @Override
    public String toString() {
        return stringWriter.toString();
    }
}