package com.gentics.mesh.plugin.export.filter;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.gentics.mesh.plugin.export.util.Utils;
import com.gentics.mesh.plugin.export.model.AbstractCsvMappingDefinition;
import com.gentics.mesh.plugin.export.model.IFilter;
import io.vertx.core.json.JsonObject;

import javax.validation.constraints.NotNull;
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

    @Override
    public boolean isAllowed(JsonObject value) {
        Optional<Object> obj = Utils.getValueByPath(value, Utils.getPathAsList(this.path));
        if (obj.isPresent()) {
            return Objects.equals(this.value, obj.get());
        } else {
            return this.value == null;
        }
    }
}
