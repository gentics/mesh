package com.gentics.mesh.core.data.impl;

import com.gentics.mesh.core.data.MeshNodeFieldContainer;
import com.gentics.mesh.core.data.node.field.impl.nesting.MicroschemaFieldImpl;
import com.gentics.mesh.core.data.node.field.nesting.MicroschemaField;
import com.gentics.mesh.core.data.relationship.MeshRelationships;
import com.gentics.mesh.core.rest.common.FieldTypes;
import com.gentics.mesh.core.rest.node.field.Field;
import com.gentics.mesh.core.rest.node.field.impl.HTMLFieldImpl;
import com.gentics.mesh.core.rest.node.field.impl.StringFieldImpl;
import com.gentics.mesh.core.rest.schema.BooleanFieldSchema;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.HTMLFieldSchema;
import com.gentics.mesh.core.rest.schema.ListFieldSchema;
import com.gentics.mesh.core.rest.schema.NodeFieldSchema;
import com.gentics.mesh.core.rest.schema.NumberFieldSchema;
import com.gentics.mesh.core.rest.schema.SelectFieldSchema;
import com.gentics.mesh.core.rest.schema.StringFieldSchema;

public class MeshNodeFieldContainerImpl extends AbstractFieldContainerImpl implements MeshNodeFieldContainer {

	@Override
	public MicroschemaField createMicroschema(String key) {
		MicroschemaFieldImpl field = getGraph().addFramedVertex(MicroschemaFieldImpl.class);
		field.setFieldKey(key);
		linkOut(field, MeshRelationships.HAS_FIELD);
		return field;
	}

	@Override
	public MicroschemaField getMicroschema(String key) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Field getRestField(String fieldKey, FieldSchema fieldSchema) {
		FieldTypes type = FieldTypes.valueByName(fieldSchema.getType());
		if (FieldTypes.STRING.equals(type)) {
			StringFieldSchema stringFieldSchema = (StringFieldSchema) fieldSchema;
			com.gentics.mesh.core.data.node.field.basic.StringField graphStringField = new com.gentics.mesh.core.data.node.field.impl.basic.StringFieldImpl(
					fieldKey, this);
			StringFieldImpl stringField = new StringFieldImpl();
			String text = graphStringField.getString();
			stringField.setText(text == null ? "" : text);
			return stringField;
		}

		if (FieldTypes.NUMBER.equals(type)) {
			NumberFieldSchema numberFieldSchema = (NumberFieldSchema) fieldSchema;
		}

		if (FieldTypes.BOOLEAN.equals(type)) {
			BooleanFieldSchema booleanFieldSchema = (BooleanFieldSchema) fieldSchema;
		}

		if (FieldTypes.NODE.equals(type)) {
			NodeFieldSchema nodeFieldSchema = (NodeFieldSchema) fieldSchema;
		}

		if (FieldTypes.HTML.equals(type)) {
			HTMLFieldSchema htmlFieldSchema = (HTMLFieldSchema) fieldSchema;
			com.gentics.mesh.core.data.node.field.basic.HTMLField graphStringField = new com.gentics.mesh.core.data.node.field.impl.basic.HTMLFieldImpl(
					fieldKey, this);
			HTMLFieldImpl htmlField = new HTMLFieldImpl();
			String text = graphStringField.getHTML();
			htmlField.setHTML(text == null ? "" : text);
			return htmlField;
		}

		if (FieldTypes.LIST.equals(type)) {
			ListFieldSchema listFieldSchema = (ListFieldSchema) fieldSchema;
			//			String listType = listFielSchema.getListType();
		}
		if (FieldTypes.SELECT.equals(type)) {
			SelectFieldSchema selectFieldSchema = (SelectFieldSchema) fieldSchema;
		}

		if (FieldTypes.MICROSCHEMA.equals(type)) {
			NumberFieldSchema numberFieldSchema = (NumberFieldSchema) fieldSchema;
		}
		System.out.println(fieldSchema.getClass().getName());
		// fieldSchema.getType()
		// restNode.getFields().add(e)
		// restNode.addProperty(d, value);

		return null;
	}
}
