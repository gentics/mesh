package com.gentics.mesh.core.data.impl;

import io.vertx.ext.web.RoutingContext;

import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.StringUtils;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.data.NodeFieldContainer;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.field.impl.nesting.MicroschemaFieldImpl;
import com.gentics.mesh.core.data.node.field.nesting.ListableField;
import com.gentics.mesh.core.data.node.field.nesting.MicroschemaField;
import com.gentics.mesh.core.data.relationship.MeshRelationships;
import com.gentics.mesh.core.data.service.I18NService;
import com.gentics.mesh.core.rest.common.FieldTypes;
import com.gentics.mesh.core.rest.error.HttpStatusCodeErrorException;
import com.gentics.mesh.core.rest.node.field.BooleanField;
import com.gentics.mesh.core.rest.node.field.DateField;
import com.gentics.mesh.core.rest.node.field.Field;
import com.gentics.mesh.core.rest.node.field.HTMLField;
import com.gentics.mesh.core.rest.node.field.ListField;
import com.gentics.mesh.core.rest.node.field.NodeField;
import com.gentics.mesh.core.rest.node.field.NumberField;
import com.gentics.mesh.core.rest.node.field.SelectField;
import com.gentics.mesh.core.rest.node.field.StringField;
import com.gentics.mesh.core.rest.node.field.impl.BooleanFieldImpl;
import com.gentics.mesh.core.rest.node.field.impl.DateFieldImpl;
import com.gentics.mesh.core.rest.node.field.impl.HTMLFieldImpl;
import com.gentics.mesh.core.rest.node.field.impl.ListFieldImpl;
import com.gentics.mesh.core.rest.node.field.impl.NodeFieldImpl;
import com.gentics.mesh.core.rest.node.field.impl.NumberFieldImpl;
import com.gentics.mesh.core.rest.node.field.impl.SelectFieldImpl;
import com.gentics.mesh.core.rest.node.field.impl.StringFieldImpl;
import com.gentics.mesh.core.rest.schema.BooleanFieldSchema;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.HTMLFieldSchema;
import com.gentics.mesh.core.rest.schema.ListFieldSchema;
import com.gentics.mesh.core.rest.schema.NodeFieldSchema;
import com.gentics.mesh.core.rest.schema.NumberFieldSchema;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.SelectFieldSchema;
import com.gentics.mesh.core.rest.schema.StringFieldSchema;
import com.gentics.mesh.error.MeshSchemaException;

public class NodeFieldContainerImpl extends AbstractFieldContainerImpl implements NodeFieldContainer {

	@Override
	public MicroschemaField createMicroschema(String key) {
		MicroschemaFieldImpl field = getGraph().addFramedVertex(MicroschemaFieldImpl.class);
		field.setFieldKey(key);
		linkOut(field, MeshRelationships.HAS_FIELD);
		return field;
	}

	@Override
	public void setFieldFromRest(RoutingContext rc, Map<String, Field> fields, Schema schema) throws MeshSchemaException {

		for (Entry<String, ? extends FieldSchema> entry : schema.getFields().entrySet()) {
			String key = entry.getKey();
			Field field = fields.get(key);
			if (field == null) {
				throw new MeshSchemaException("Could not find value for schema field with key {" + key + "}");
			}
			fields.remove(key);

			FieldTypes type = FieldTypes.valueByName(field.getType());
			switch (type) {
			case HTML:
				HTMLField htmlField = (HTMLFieldImpl) field;
				createHTML(key).setHTML(htmlField.getHTML());
				break;
			case STRING:
				StringField stringField = (StringFieldImpl) field;
				createString(key).setString(stringField.getString());
				break;
			case NUMBER:
				NumberField numberField = (NumberFieldImpl) field;
				createNumber(key).setNumber(numberField.getNumber());
				break;
			case BOOLEAN:
				BooleanField booleanField = (BooleanFieldImpl) field;
				createBoolean(key).setBoolean(booleanField.getValue());
				break;
			case DATE:
				DateField dateField = (DateFieldImpl) field;
				createDate(key).setDate(dateField.getDate());
				break;
			case NODE:
				NodeField nodeField = (NodeFieldImpl) field;
				BootstrapInitializer.getBoot().nodeRoot().findByUuid(nodeField.getUuid(), rh -> {
					Node node = rh.result();	
					createNode(key, node);
				});
				//TODO check node permissions
				break;
			case LIST:
				ListField restListField = (ListFieldImpl) field;
				com.gentics.mesh.core.data.node.field.nesting.ListField<ListableField> listField = createList(key);
				break;
			case SELECT:
				SelectField restSelectField = (SelectFieldImpl) field;
				com.gentics.mesh.core.data.node.field.nesting.SelectField<ListableField> selectField = createSelect(key);
				// TODO impl
				break;
			case MICROSCHEMA:
				com.gentics.mesh.core.rest.node.field.MicroschemaField restMicroschemaField = (com.gentics.mesh.core.rest.node.field.impl.MicroschemaFieldImpl) field;
				MicroschemaField microschemaField = createMicroschema(key);
				// TODO impl
				break;
			}

		}
		String extraFields = "";
		for (String key : fields.keySet()) {
			extraFields += "[" + key + "]";
		}
		if (!StringUtils.isEmpty(extraFields)) {
			throw new HttpStatusCodeErrorException(400, I18NService.getI18n().get(rc, "node_unhandled_fields", schema.getName(), extraFields));
			//throw new MeshSchemaException("The following fields were not specified within the {" + schema.getName() + "} schema: " + extraFields);
		}
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
			//TODO validate found fields has same type as schema 
			com.gentics.mesh.core.data.node.field.basic.StringField graphStringField = new com.gentics.mesh.core.data.node.field.impl.basic.StringFieldImpl(
					fieldKey, this);
			StringFieldImpl stringField = new StringFieldImpl();
			String text = graphStringField.getString();
			stringField.setString(text == null ? "" : text);
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
			// String listType = listFielSchema.getListType();
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
