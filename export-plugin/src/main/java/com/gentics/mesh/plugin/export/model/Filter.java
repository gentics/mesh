package com.gentics.mesh.plugin.export.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.gentics.mesh.plugin.export.filter.EqualsFilter;
import com.gentics.mesh.plugin.export.filter.RangeFilter;
import com.gentics.mesh.plugin.export.validaton.FilterConsistency;
import io.vertx.core.json.JsonObject;

import javax.validation.Valid;
import java.util.List;

@FilterConsistency
public class Filter implements IFilter {

    @Valid
    @JsonProperty
    public List<Filter> and;

    @Valid
    @JsonProperty
    public EqualsFilter equals;

    @Valid
    @JsonProperty
    public Filter not;

    @Valid
    @JsonProperty
    public List<Filter> or;

    @Valid
    @JsonProperty
    public RangeFilter range;

    @Override
    public boolean isAllowed(AbstractCsvMappingDefinition definition, JsonObject value) {
        IFilter filter = null;

        if (this.and != null) {
            filter = (AbstractCsvMappingDefinition _definition, JsonObject _value) -> {
                boolean flag = true;
                for (Filter f : this.and) {
                    if (f != null && !f.isAllowed(_definition, _value)) {
                        flag = false;
                        break;
                    }
                }
                return flag;
            };
        } else if (this.equals != null) {
            filter = this.equals;
        } else if (this.not != null) {
            filter = (AbstractCsvMappingDefinition _definition, JsonObject _value) -> !this.not.isAllowed(_definition, _value);
        } else if (this.or != null) {
            filter = (AbstractCsvMappingDefinition _definition, JsonObject _value) -> {
                boolean flag = false;
                for (Filter f : this.or) {
                    if (f != null && f.isAllowed(_definition, _value)) {
                        flag = true;
                        break;
                    }
                }
                return flag;
            };
        } else if (this.range != null) {
            filter = range;
        }

        if (filter != null) {
            return filter.isAllowed(definition, value);
        }

        return false;
    }
}
