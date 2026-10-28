package com.gentics.mesh.plugin.export.model;

import java.util.ArrayList;
import java.util.Collection;

public class CsvMatrix extends ArrayList<ColumnGroup> {

    public CsvMatrix() {
    }

    public CsvMatrix(Collection<? extends ColumnGroup> c) {
        super(c);
    }
}
