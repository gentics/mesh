package com.gentics.mesh.plugin.export.filter;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.gentics.mesh.plugin.export.model.IFilter;
import com.gentics.mesh.plugin.export.util.Utils;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class InFilter implements IFilter {

	@NotNull
	@JsonProperty
	private String path;

	@NotNull
	@NotEmpty
	@JsonProperty
	private List<Object> values;

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

	@Override
	public boolean isAllowed(JsonObject value) {
		Optional<Object> obj = Utils.getValueByPath(value, Utils.getPathAsList(this.path));
		if (!obj.isPresent() || this.values == null || this.values.isEmpty()) {
			return false;
		}

		return this.values.stream().anyMatch(singleValue -> Objects.equals(singleValue, obj.get()));
	}
}
