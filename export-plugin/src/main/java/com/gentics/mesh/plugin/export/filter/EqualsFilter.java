package com.gentics.mesh.plugin.export.filter;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.gentics.mesh.plugin.export.Utils;
import com.gentics.mesh.plugin.export.model.AbstractCsvMappingDefinition;
import com.gentics.mesh.plugin.export.model.IFilter;
import io.vertx.core.json.JsonObject;

import javax.validation.constraints.NotNull;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class EqualsFilter implements IFilter {

    @NotNull
    @JsonProperty
    private String path;

    @JsonProperty
    private Object value;

    public EqualsFilter() {
    }

    public EqualsFilter(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public List<String> getPathAsList() {
        return Arrays.asList(this.path.split("\\."));
    }

    @Override
    public boolean isAllowed(AbstractCsvMappingDefinition definition, JsonObject value) {
        Optional<Object> obj = Utils.getValueByPath(value, this.getPathAsList());
        if (obj.isPresent()) {
            return Objects.equals(this.value, obj.get());
        } else {
            return this.value == null;
        }
    }
}
