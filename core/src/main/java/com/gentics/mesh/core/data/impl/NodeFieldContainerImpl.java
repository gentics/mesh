package com.gentics.mesh.core.data.impl;

import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import io.vertx.ext.web.RoutingContext;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang3.StringUtils;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.data.NodeFieldContainer;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.field.impl.nesting.MicroschemaFieldImpl;
import com.gentics.mesh.core.data.node.field.list.BooleanFieldList;
import com.gentics.mesh.core.data.node.field.list.DateFieldList;
import com.gentics.mesh.core.data.node.field.list.HtmlFieldList;
import com.gentics.mesh.core.data.node.field.list.MicroschemaFieldList;
import com.gentics.mesh.core.data.node.field.list.NodeFieldList;
import com.gentics.mesh.core.data.node.field.list.NumberFieldList;
import com.gentics.mesh.core.data.node.field.list.StringFieldList;
import com.gentics.mesh.core.data.node.field.nesting.ListableField;
import com.gentics.mesh.core.data.node.field.nesting.MicroschemaField;
import com.gentics.mesh.core.data.relationship.MeshRelationships;
import com.gentics.mesh.core.data.service.I18NService;
import com.gentics.mesh.core.rest.common.FieldTypes;
import com.gentics.mesh.core.rest.error.HttpStatusCodeErrorException;
import com.gentics.mesh.core.rest.node.field.BooleanField;
import com.gentics.mesh.core.rest.node.field.DateField;
import com.gentics.mesh.core.rest.node.field.Field;
import com.gentics.mesh.core.rest.node.field.HtmlField;
import com.gentics.mesh.core.rest.node.field.NodeField;
import com.gentics.mesh.core.rest.node.field.NumberField;
import com.gentics.mesh.core.rest.node.field.SelectField;
import com.gentics.mesh.core.rest.node.field.StringField;
import com.gentics.mesh.core.rest.node.field.impl.BooleanFieldImpl;
import com.gentics.mesh.core.rest.node.field.impl.DateFieldImpl;
import com.gentics.mesh.core.rest.node.field.impl.HtmlFieldImpl;
import com.gentics.mesh.core.rest.node.field.impl.NodeFieldImpl;
import com.gentics.mesh.core.rest.node.field.impl.NumberFieldImpl;
import com.gentics.mesh.core.rest.node.field.impl.SelectFieldImpl;
import com.gentics.mesh.core.rest.node.field.impl.StringFieldImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.BooleanFieldListImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.DateFieldListImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.HtmlFieldListImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.MicroschemaFieldListImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.NodeFieldListImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.NodeFieldListItem;
import com.gentics.mesh.core.rest.node.field.list.impl.NumberFieldListImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.StringFieldListImpl;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.ListFieldSchema;
import com.gentics.mesh.core.rest.schema.NumberFieldSchema;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.SelectFieldSchema;
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

		BootstrapInitializer boot = BootstrapInitializer.getBoot();

		for (FieldSchema entry : schema.getFields()) {
			String key = entry.getName();
			Field field = fields.get(key);

			if (field == null && entry.isRequired()) {
				throw new MeshSchemaException("Could not find value for required schema field with key {" + key + "}");
			} else if (field == null) {
				continue;
			}
			fields.remove(key);

			FieldTypes type = FieldTypes.valueByName(field.getType());
			switch (type) {
			case HTML:
				HtmlField htmlField = (HtmlFieldImpl) field;
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
				if (field instanceof NodeFieldListImpl) {
					NodeFieldListImpl nodeList = (NodeFieldListImpl) field;
					AtomicInteger integer = new AtomicInteger();
					NodeFieldList graphNodeList = createNodeList(key);

					// Add the listed items
					for (NodeFieldListItem item : nodeList.getList()) {
						boot.nodeRoot().findByUuid(item.getUuid(), rh -> {
							if (rh.failed() || rh.result() == null) {
								//TODO log info that node was not found or throw error? -> throw error
							} else {
								Node node = rh.result();
								graphNodeList.createNode(String.valueOf(integer.incrementAndGet()), node);
							}
						});
					}
				} else if (field instanceof StringFieldListImpl) {
					StringFieldListImpl stringList = (StringFieldListImpl) field;
					StringFieldList graphStringList = createStringList(key);

					for (String item : stringList.getList()) {
						graphStringList.createString(item);
					}
				} else if (field instanceof HtmlFieldListImpl) {
					HtmlFieldListImpl htmlList = (HtmlFieldListImpl) field;
					HtmlFieldList graphHtmlList = createHTMLList(key);

					for (String item : htmlList.getList()) {
						graphHtmlList.createHTML(item);
					}
				} else if (field instanceof NumberFieldListImpl) {
					NumberFieldListImpl numberList = (NumberFieldListImpl) field;
					NumberFieldList graphNumberList = createNumberList(key);

					for (String item : numberList.getList()) {
						graphNumberList.createNumber(item);
					}
				} else if (field instanceof BooleanFieldListImpl) {

					BooleanFieldListImpl booleanList = (BooleanFieldListImpl) field;
					BooleanFieldList graphBooleanList = createBooleanList(key);

					for (Boolean item : booleanList.getList()) {
						graphBooleanList.createBoolean(item);
					}
				} else if (field instanceof DateFieldListImpl) {
					DateFieldListImpl dateList = (DateFieldListImpl) field;
					DateFieldList graphDateList = createDateList(key);

					for (String item : dateList.getList()) {
						graphDateList.createDate(item);
					}
				} else if (field instanceof MicroschemaFieldListImpl) {
					throw new NotImplementedException();
				} else {
					//TODO unknown type - throw better error
					throw new NotImplementedException();
				}
				break;
			case SELECT:
				SelectField restSelectField = (SelectFieldImpl) field;
				com.gentics.mesh.core.data.node.field.nesting.SelectField<ListableField> selectField = createSelect(key);
				// TODO impl
				throw new NotImplementedException();
				//				break;
			case MICROSCHEMA:
				com.gentics.mesh.core.rest.node.field.MicroschemaField restMicroschemaField = (com.gentics.mesh.core.rest.node.field.impl.MicroschemaFieldImpl) field;
				MicroschemaField microschemaField = createMicroschema(key);
				// TODO impl
				throw new NotImplementedException();
				//				break;
			}

		}
		String extraFields = "";
		for (String key : fields.keySet()) {
			extraFields += "[" + key + "]";
		}
		if (!StringUtils.isEmpty(extraFields)) {
			throw new HttpStatusCodeErrorException(BAD_REQUEST, I18NService.getI18n().get(rc, "node_unhandled_fields", schema.getName(), extraFields));
			//throw new MeshSchemaException("The following fields were not specified within the {" + schema.getName() + "} schema: " + extraFields);
		}
	}

	@Override
	public MicroschemaField getMicroschema(String key) {
		throw new NotImplementedException();
	}

	@Override
	public Field getRestField(String fieldKey, FieldSchema fieldSchema) {
		FieldTypes type = FieldTypes.valueByName(fieldSchema.getType());
		if (FieldTypes.STRING.equals(type)) {
			//TODO validate found fields has same type as schema 
			com.gentics.mesh.core.data.node.field.basic.StringField graphStringField = new com.gentics.mesh.core.data.node.field.impl.basic.StringFieldImpl(
					fieldKey, this);
			//TODO handle null across all types

			StringFieldImpl stringField = new StringFieldImpl();
			String text = graphStringField.getString();
			stringField.setString(text == null ? "" : text);
			return stringField;
		}

		if (FieldTypes.NUMBER.equals(type)) {
			com.gentics.mesh.core.data.node.field.basic.NumberField graphNumberField = getNumber(fieldKey);
			//TODO handle null across all types
			if (graphNumberField != null) {
				NumberFieldImpl numberField = new NumberFieldImpl();
				numberField.setNumber(graphNumberField.getNumber());
				return numberField;
			}
		}

		if (FieldTypes.DATE.equals(type)) {
			com.gentics.mesh.core.data.node.field.basic.DateField graphDateField = getDate(fieldKey);
			//TODO handle null across all types
			if (graphDateField != null) {
				DateFieldImpl dateField = new DateFieldImpl();
				dateField.setDate(graphDateField.getDate());
				return dateField;
			}
		}

		if (FieldTypes.BOOLEAN.equals(type)) {
			com.gentics.mesh.core.data.node.field.basic.BooleanField graphNumberField = getBoolean(fieldKey);
			//TODO handle null across all types
			if (graphNumberField != null) {
				BooleanFieldImpl booleanField = new BooleanFieldImpl();
				booleanField.setValue(graphNumberField.getBoolean());
				return booleanField;
			}
		}

		if (FieldTypes.NODE.equals(type)) {
			com.gentics.mesh.core.data.node.field.nesting.NodeField graphNodeField = getNode(fieldKey);
			//TODO handle null across all types
			if (graphNodeField != null && graphNodeField.getNode() != null) {
				NodeFieldImpl nodeField = new NodeFieldImpl();
				nodeField.setUuid(graphNodeField.getNode().getUuid());
				return nodeField;
			}
		}

		if (FieldTypes.HTML.equals(type)) {
			com.gentics.mesh.core.data.node.field.basic.HtmlField graphStringField = new com.gentics.mesh.core.data.node.field.impl.basic.HtmlFieldImpl(
					fieldKey, this);
			HtmlFieldImpl htmlField = new HtmlFieldImpl();
			String text = graphStringField.getHTML();
			htmlField.setHTML(text == null ? "" : text);
			return htmlField;
		}

		if (FieldTypes.LIST.equals(type)) {
			ListFieldSchema listFieldSchema = (ListFieldSchema) fieldSchema;

			switch (listFieldSchema.getListType()) {
			case NodeFieldList.TYPE:
				NodeFieldListImpl restNodeFieldList = new NodeFieldListImpl();
				NodeFieldList nodeFieldList = getNodeList(fieldKey);
				if (nodeFieldList == null) {
					return null;
				} else {
					for (com.gentics.mesh.core.data.node.field.nesting.NodeField item : nodeFieldList.getList()) {
						// Create the rest field and populate the fields
						NodeFieldListItem listItem = new NodeFieldListItem(item.getUuid());
						restNodeFieldList.add(listItem);
					}
					return restNodeFieldList;
				}
			case NumberFieldList.TYPE:
				NumberFieldListImpl numberList = new NumberFieldListImpl();
				NumberFieldList numberFieldList = getNumberList(fieldKey);
				for (com.gentics.mesh.core.data.node.field.basic.NumberField item : numberFieldList.getList()) {
					numberList.add(item.getNumber());
				}
				return numberList;
			case BooleanFieldList.TYPE:
				BooleanFieldListImpl booleanList = new BooleanFieldListImpl();
				BooleanFieldList booleanFieldList = getBooleanList(fieldKey);
				for (com.gentics.mesh.core.data.node.field.basic.BooleanField item : booleanFieldList.getList()) {
					booleanList.add(item.getBoolean());
				}
				return booleanList;
			case HtmlFieldList.TYPE:
				HtmlFieldListImpl htmlList = new HtmlFieldListImpl();
				HtmlFieldList htmlFieldList = getHTMLList(fieldKey);
				for (com.gentics.mesh.core.data.node.field.basic.HtmlField item : htmlFieldList.getList()) {
					htmlList.add(item.getHTML());
				}
				return htmlList;
			case MicroschemaFieldList.TYPE:
				MicroschemaFieldListImpl microschemaList = new MicroschemaFieldListImpl();
				return microschemaList;
			case StringFieldList.TYPE:
				StringFieldListImpl stringList = new StringFieldListImpl();
				StringFieldList stringFieldList = getStringList(fieldKey);
				for (com.gentics.mesh.core.data.node.field.basic.StringField item : stringFieldList.getList()) {
					stringList.add(item.getString());
				}
				return stringList;
			case DateFieldList.TYPE:
				DateFieldListImpl dateList = new DateFieldListImpl();
				DateFieldList dateFieldList = getDateList(fieldKey);
				for (com.gentics.mesh.core.data.node.field.basic.DateField item : dateFieldList.getList()) {
					dateList.add(item.getDate());
				}
				return dateList;
			}
			// String listType = listFielSchema.getListType();
		}
		if (FieldTypes.SELECT.equals(type)) {
			SelectFieldSchema selectFieldSchema = (SelectFieldSchema) fieldSchema;
			//			throw new NotImplementedException();

		}

		if (FieldTypes.MICROSCHEMA.equals(type)) {
			NumberFieldSchema numberFieldSchema = (NumberFieldSchema) fieldSchema;
			throw new NotImplementedException();

		}
		// fieldSchema.getType()
		// restNode.getFields().add(e)
		// restNode.addProperty(d, value);

		return null;
	}
}
