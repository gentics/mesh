package com.gentics.mesh.plugin.export.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.gentics.mesh.plugin.export.filter.EqualsFilter;
import com.gentics.mesh.plugin.export.filter.InFilter;
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

    @Valid
    @JsonProperty
    public InFilter in;

    @Override
    public boolean isAllowed(JsonObject value) {
        IFilter filter = null;

        if (this.and != null) {
            filter = (JsonObject _value) -> {
                boolean flag = true;
                for (Filter f : this.and) {
                    if (f != null && !f.isAllowed(_value)) {
                        flag = false;
                        break;
                    }
                }
                return flag;
            };
        } else if (this.equals != null) {
            filter = this.equals;
        } else if (this.not != null) {
            filter = (JsonObject _value) -> !this.not.isAllowed(_value);
        } else if (this.or != null) {
            filter = (JsonObject _value) -> {
                boolean flag = false;
                for (Filter f : this.or) {
                    if (f != null && f.isAllowed(_value)) {
                        flag = true;
                        break;
                    }
                }
                return flag;
            };
        } else if (this.range != null) {
            filter = range;
        } else if (this.in != null) {
            filter = in;
        }

        if (filter != null) {
            return filter.isAllowed(value);
        }

        return false;
    }
}
