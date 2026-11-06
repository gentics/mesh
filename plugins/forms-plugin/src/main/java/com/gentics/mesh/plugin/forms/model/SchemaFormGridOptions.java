package com.gentics.mesh.plugin.forms.model;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;

/**
 * Additions of a {@link FormElement control} from form-grid, as defined in the schema's {@code formGridOptions} property.
 * Contains {@code selectOptions} (static choices) and {@code defaultValue} (pre-fill value).
 * This data will later be migrated into the form's {@code data} block.
 */
public class SchemaFormGridOptions {

	private List<ValueListItem> selectOptions;

	private DefaultValue defaultValue;

	private Boolean customOption;

	protected Map<String, Object> properties = new LinkedHashMap<>();

	public List<ValueListItem> getSelectOptions() {
		return selectOptions;
	}

	public SchemaFormGridOptions setSelectOptions(List<ValueListItem> selectOptions) {
		this.selectOptions = selectOptions;
		return this;
	}

	public DefaultValue getDefaultValue() {
		return defaultValue;
	}

	public SchemaFormGridOptions setDefaultValue(DefaultValue defaultValue) {
		this.defaultValue = defaultValue;
		return this;
	}

	public Boolean getCustomOption() {
		return customOption;
	}

	public SchemaFormGridOptions setCustomOption(Boolean customOption) {
		this.customOption = customOption;
		return this;
	}

	/**
	 * Get the map of additional properties.
	 * @return The map of additional properties
	 */
	@JsonAnyGetter
	public Map<String, Object> getAny() {
		if (properties == null) {
			properties = new LinkedHashMap<>();
		}

		return properties;
	}

	/**
	 * Set the specified additional property to the given value.
	 *
	 * <p>
	 *     This will store {@code value} under {@code key} in the
	 *     {@link #properties} map.
	 * </p>
	 *
	 * @param key The name of the additional property
	 * @param value The value of the additional property
	 */
	@JsonAnySetter
	public void setProperty(String key, Object value) {
		if (StringUtils.isNotBlank(key)) {
			getAny().put(key, value);
		}
	}
}
