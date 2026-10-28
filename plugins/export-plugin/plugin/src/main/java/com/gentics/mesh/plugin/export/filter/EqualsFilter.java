package com.gentics.mesh.plugin.export.filter;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.gentics.mesh.plugin.export.util.Utils;
import com.gentics.mesh.plugin.export.model.AbstractCsvMappingDefinition;
import com.gentics.mesh.plugin.export.model.IFilter;
import com.gentics.mesh.plugin.export.validaton.EqualsFilterConsistency;
import io.vertx.core.json.JsonObject;

import javax.validation.constraints.NotNull;
import java.util.Objects;
import java.util.Optional;

@EqualsFilterConsistency
public class EqualsFilter implements IFilter {

    @NotNull
    @JsonProperty
    private String path;

    @JsonProperty
    private Object value;

    @JsonProperty
    private String valuePath;

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

    public String getValuePath() {
        return valuePath;
    }

    public EqualsFilter setValuePath(String valuePath) {
        this.valuePath = valuePath;
        return this;
    }

    @Override
    public boolean isAllowed(JsonObject source) {
        Optional<Object> obj = Utils.getValueByPath(source, Utils.getPathAsList(this.path));

        if (this.valuePath != null) {
            Optional<Object> value = Utils.getValueByPath(source, Utils.getPathAsList(this.valuePath));
            if (obj.isPresent() && value.isPresent()) {
                return Objects.equals(obj.get(), value.get());
            }
            return !obj.isPresent() && !value.isPresent();
        }

        return Objects.equals(obj.orElse(null), this.value);
    }
}
