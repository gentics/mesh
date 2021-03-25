package com.gentics.mesh.plugin.export.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.plugin.export.filter.EqualsFilter;
import com.gentics.mesh.plugin.export.filter.InFilter;
import com.gentics.mesh.plugin.export.filter.RangeFilter;
import com.gentics.mesh.plugin.export.util.Utils;
import com.gentics.mesh.plugin.export.validaton.FilterConsistency;
import io.vertx.core.json.JsonObject;

import javax.validation.Valid;
import java.util.List;

@FilterConsistency
public class Filter implements IFilter {

    @Valid
    @JsonProperty
    @JsonPropertyDescription("Meta filter to check if all provided filters are matching.")
    public List<Filter> and;

    @Valid
    @JsonProperty
    @JsonPropertyDescription("Meta filter to check if any provided filters are matching.")
    public List<Filter> or;

    @Valid
    @JsonProperty
    @JsonPropertyDescription("Meta filter which inverts the output of the provided filter.")
    public Filter not;

    @JsonProperty
    @JsonPropertyDescription("Filter to check if the value in the specified path is empty (non-existent/null).")
    public String empty;

    @JsonProperty
    @JsonPropertyDescription("Filter to check if the value in the specified path exists (not null).")
    public String exists;

    @Valid
    @JsonProperty
    @JsonPropertyDescription("Filter to check if the specified values are equal.")
    public EqualsFilter equals;

    @Valid
    @JsonProperty
    @JsonPropertyDescription("Filter to check if the specified value are within a certain range.")
    public RangeFilter range;

    @Valid
    @JsonProperty
    @JsonPropertyDescription("Filter to check if the specified value is found in a collection.")
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
        } else if (this.not != null) {
            filter = (JsonObject _value) -> !this.not.isAllowed(_value);
        } else if (this.empty != null) {
            filter = (JsonObject _value) -> !Utils.getValueByPath(_value, Utils.getPathAsList(this.empty)).isPresent();
        } else if (this.exists != null) {
            filter = (JsonObject _value) -> Utils.getValueByPath(_value, Utils.getPathAsList(this.exists)).isPresent();
        } else if (this.equals != null) {
            filter = this.equals;
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
