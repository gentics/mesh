package com.gentics.mesh.plugin.export.model;

import java.util.ArrayList;
import java.util.Collection;

public class ColumnGroup extends ArrayList<Column> {

    public ColumnGroup() {
    }

    public ColumnGroup(Column column) {
        this.add(column);
    }

    public ColumnGroup(Collection<? extends Column> c) {
        super(c);
    }

    public boolean isMultiColumn() {
        return this.size() > 1;
    }
}
