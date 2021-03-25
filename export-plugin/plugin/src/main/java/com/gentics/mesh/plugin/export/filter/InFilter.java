package com.gentics.mesh.plugin.export.filter;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.gentics.mesh.plugin.export.model.IFilter;
import com.gentics.mesh.plugin.export.util.Utils;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class InFilter implements IFilter {

	private static final Logger log = LoggerFactory.getLogger(InFilter.class);

	@NotNull
	@JsonProperty
	private String path;

	@JsonProperty
	private List<Object> values;

	@JsonProperty
	private String valuesPath;

	@JsonProperty
	private boolean strict = false;

	public InFilter() {
	}

	public InFilter(String path) {
		this.path = path;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public List<Object> getValues() {
		return values;
	}

	public void setValues(List<Object> values) {
		this.values = values;
	}

	public String getValuesPath() {
		return valuesPath;
	}

	public InFilter setValuesPath(String valuesPath) {
		this.valuesPath = valuesPath;
		return this;
	}

	@Override
	public boolean isAllowed(JsonObject source) {
		Optional<Object> obj = Utils.getValueByPath(source, Utils.getPathAsList(this.path));
		if (!obj.isPresent()) {
			return false;
		}

		Iterable<?> values;

		if (this.valuesPath != null) {
			Optional<Object> tmp = Utils.getValueByPath(source, Utils.getPathAsList(this.valuesPath));

			if (!tmp.isPresent() || !(tmp.get() instanceof Iterable<?>)) {
				return false;
			}
			values = (Iterable<?>) tmp.get();
		} else {
			values = this.values;
		}

		if (this.strict && (obj.get() instanceof JsonObject || obj.get() instanceof JsonArray)) {
			log.warn("Strict mode for in-filter active and object on path {} was not a simple value, but instead {}!", this.path, obj.get());
			return false;
		}

		if (obj.get() instanceof JsonArray) {
			for (Object tmp : (JsonArray) obj.get()) {
				for (Object singleValue : values) {
					if (Objects.equals(tmp, singleValue)) {
						return true;
					}
				}
			}
		} else {
			Object compare = obj.get();
			for (Object singleValue : values) {
				if (Objects.equals(compare, singleValue)) {
					return true;
				}
			}
		}

		return false;
	}
}
