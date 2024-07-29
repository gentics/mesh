package com.gentics.mesh.plugin.forms.model;

import java.util.List;
import java.util.function.Function;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.gentics.mesh.core.rest.common.FieldContainer;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.field.impl.BooleanFieldImpl;
import com.gentics.mesh.core.rest.schema.FieldSchemaContainer;
import com.gentics.mesh.core.rest.schema.impl.BooleanFieldSchemaImpl;

import io.vertx.core.MultiMap;
import io.vertx.core.json.JsonObject;

/**
 * Formpage element, which is a container for other elements. Stores boolean flag in mesh, which is true, when all nested elements were successfully posted
 */
public class FormpageElement extends FormElement {

	@JsonProperty
	private String description;

	public FormpageElement() {
	}

	protected FormpageElement(FormpageElement other, boolean deep) {
		super(other, deep);

		this.description = other.description;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	@Override
	public void addFieldSchema(FieldSchemaContainer container) {
		container.addField(new BooleanFieldSchemaImpl().setName(getName()).setLabel(getName()));
	}

	@Override
	public void addField(FieldContainer container, MultiMap postedData, boolean errors) {
		String value = getSingleFormData(postedData);
		if ("1".equals(value)) {
			container.getFields().put(getName(), new BooleanFieldImpl().setValue(!errors));
		}
	}

	@Override
	public void addFormData(JsonObject object, FieldContainer container, Function<FormElement, String> key) {
		BooleanFieldImpl field = container.getFields().getBooleanField(getName());
		if (field != null && field.getValue() == Boolean.TRUE) {
			object.put(key.apply(this), "1");
		}
	}

	@Override
	public List<String> getMultiFormData(MultiMap postedData) {
		return null;
	}

	@Override
	public boolean isCompleted(NodeResponse formDataNode) {
		BooleanFieldImpl field = formDataNode.getFields().getBooleanField(getName());
		if (field == null) {
			return false;
		} else {
			return field.getValue() == Boolean.TRUE;
		}
	}

	@Override
	protected FormElement clone(boolean deep) {
		return new FormpageElement(this, deep);
	}
}
