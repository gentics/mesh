package com.gentics.mesh.plugin.forms.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Additions of a {@link FormElement control} from form-grid, as defined in the schema's {@code formGridOptions} property.
 * Contains {@code selectOptions} (static choices) and {@code defaultValue} (pre-fill value).
 * This data will later be migrated into the form's {@code data} block.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class SchemaFormGridOptions {

	private List<ValueListItem> selectOptions;

	private Object defaultValue;

	public List<ValueListItem> getSelectOptions() {
		return selectOptions;
	}

	public SchemaFormGridOptions setSelectOptions(List<ValueListItem> selectOptions) {
		this.selectOptions = selectOptions;
		return this;
	}

	public Object getDefaultValue() {
		return defaultValue;
	}

	public SchemaFormGridOptions setDefaultValue(Object defaultValue) {
		this.defaultValue = defaultValue;
		return this;
	}
}
