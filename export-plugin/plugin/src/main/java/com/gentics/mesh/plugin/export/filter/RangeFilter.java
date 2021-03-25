package com.gentics.mesh.plugin.export.filter;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.gentics.mesh.plugin.export.util.Utils;
import com.gentics.mesh.plugin.export.model.AbstractCsvMappingDefinition;
import com.gentics.mesh.plugin.export.model.IFilter;
import com.gentics.mesh.plugin.export.validaton.RangeFilterConsistency;
import io.vertx.core.json.JsonObject;

import javax.validation.constraints.NotNull;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

@RangeFilterConsistency
public class RangeFilter implements IFilter {

    @NotNull
    @JsonProperty
    private String path;

    @JsonProperty
    public Object gt;

    @JsonProperty
    public Object gte;

    @JsonProperty
    public Object lt;

    @JsonProperty
    public Object lte;

    public RangeFilter() {
    }

    public RangeFilter(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    @Override
    public boolean isAllowed(JsonObject source) {
        if (this.gt == null
                && this.gte == null
                && this.lt == null
                && this.lte == null
        ) {
            return false;
        }

        Optional<Object> value = Utils.getValueByPath(source, Utils.getPathAsList(this.path));
        if (!value.isPresent()) {
            return false;
        }

        Object content = value.get();

        if (content instanceof Number) {
            Double num = ((Number) content).doubleValue();

            if (this.lt != null && this.lt instanceof Number) {
                if (!(num < ((Number) this.lt).doubleValue())) {
                    return false;
                }
            } else if (this.lte != null && this.lte instanceof Number) {
                if (!(num <= ((Number) this.lte).doubleValue())) {
                    return false;
                }
            }

            if (this.gt != null && this.gt instanceof Number) {
                if (!(num > ((Number) this.gt).doubleValue())) {
                    return false;
                }
            } else if (this.gte != null && this.gte instanceof Number) {
                if (!(num >= ((Number) this.gte).doubleValue())) {
                    return false;
                }
            }

            return true;
        } else if (content instanceof Instant || content instanceof String) {
            Instant date = content instanceof String ? Utils.parseIsoDate((String) content) : (Instant) content;

            if (this.lt != null && (this.lt instanceof String || this.lt instanceof Date)) {
                Instant compare = this.lt instanceof Instant ? (Instant) this.lt : Utils.parseIsoDate((String) this.lt);
                if (!(compare.compareTo(date) > 0)) {
                    return false;
                }
            } else if (this.lte != null && (this.lte instanceof String || this.lte instanceof Date)) {
                Instant compare = this.lte instanceof Instant ? (Instant) this.lte : Utils.parseIsoDate((String) this.lte);
                if (!(compare.compareTo(date) >= 0)) {
                    return false;
                }
            }

            if (this.gt != null && (this.gt instanceof String || this.gt instanceof Date)) {
                Instant compare = this.gt instanceof Instant ? (Instant) this.gt : Utils.parseIsoDate((String) this.gt);
                if (!(compare.compareTo(date) < 0)) {
                    return false;
                }
            } else if (this.gte != null && (this.gte instanceof String || this.gte instanceof Date)) {
                Instant compare = this.gte instanceof Instant ? (Instant) this.gte : Utils.parseIsoDate((String) this.gte);
                if (!(compare.compareTo(date) <= 0)) {
                    return false;
                }
            }

            return true;
        }

        return false;
    }
}
