package com.gentics.mesh.plugin.forms.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.gentics.mesh.core.rest.common.FieldContainer;
import com.gentics.mesh.core.rest.node.FieldMap;
import com.gentics.mesh.core.rest.node.field.StringField;
import io.vertx.core.json.JsonObject;

import java.util.List;
import java.util.Optional;

/**
 * Combi Element
 */
@Reportable
public class CombiElement extends ValueListElement {

	@JsonProperty("select_type")
	private String selectType;

	public CombiElement() {
	}

	protected CombiElement(CombiElement other, boolean deep) {
		super(other, deep);

		this.selectType = other.selectType;
	}

	/**
	 * Get the select_type
	 * @return select_type
	 */
	@JsonProperty("select_type")
	public String getSelectType() {
		return selectType;
	}

	@JsonProperty("select_type")
	public void setSelectType(String selectType) {
		this.selectType = selectType;
	}

	@Override
	public boolean isMultivalue() {
		return "checkbox".equalsIgnoreCase(getSelectType());
	}

	@Override
	public FormElement clone(boolean deep) {
		return new CombiElement(this, deep);
	}


}
