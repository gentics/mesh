package com.gentics.mesh.plugin.export.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class Column {

    private List<String> values;

    public Column() {
        this(new ArrayList<>());
    }

    public Column(List<String> values) {
        this.values = values;
    }

    public Column(String singleValue) {
        this.values = new ArrayList<>();
        values.add(singleValue);
    }

    public List<String> getValues() {
        return values;
    }

    public Column setValues(Collection<String> values) {
        this.values.clear();
        this.values.addAll(values);
        return this;
    }

    public Column addValue(String value) {
        this.values.add(value);
        return this;
    }

    public Column addAll(Collection<String> values) {
        this.values.addAll(values);
        return this;
    }
}
