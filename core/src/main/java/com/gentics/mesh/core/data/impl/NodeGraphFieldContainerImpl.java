package com.gentics.mesh.core.data.impl;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_FIELD;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang3.StringUtils;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.data.NodeFieldContainer;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.field.impl.nesting.GraphMicroschemaFieldImpl;
import com.gentics.mesh.core.data.node.field.list.GraphBooleanFieldList;
import com.gentics.mesh.core.data.node.field.list.GraphDateFieldList;
import com.gentics.mesh.core.data.node.field.list.GraphHtmlFieldList;
import com.gentics.mesh.core.data.node.field.list.GraphMicroschemaFieldList;
import com.gentics.mesh.core.data.node.field.list.GraphNodeFieldList;
import com.gentics.mesh.core.data.node.field.list.GraphNumberFieldList;
import com.gentics.mesh.core.data.node.field.list.GraphStringFieldList;
import com.gentics.mesh.core.data.node.field.nesting.ListableGraphField;
import com.gentics.mesh.core.data.node.field.nesting.GraphMicroschemaField;
import com.gentics.mesh.core.data.relationship.GraphRelationships;
import com.gentics.mesh.core.data.service.I18NService;
import com.gentics.mesh.core.rest.common.FieldTypes;
import com.gentics.mesh.core.rest.error.HttpStatusCodeErrorException;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.field.BooleanField;
import com.gentics.mesh.core.rest.node.field.DateField;
import com.gentics.mesh.core.rest.node.field.Field;
import com.gentics.mesh.core.rest.node.field.HtmlField;
import com.gentics.mesh.core.rest.node.field.NodeField;
import com.gentics.mesh.core.rest.node.field.NodeFieldListItem;
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
import com.gentics.mesh.core.rest.node.field.list.impl.NodeFieldListItemImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.NumberFieldListImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.StringFieldListImpl;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.ListFieldSchema;
import com.gentics.mesh.core.rest.schema.NumberFieldSchema;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.SelectFieldSchema;
import com.gentics.mesh.error.MeshSchemaException;
import com.syncleus.ferma.traversals.EdgeTraversal;

import io.vertx.ext.web.RoutingContext;

public class NodeGraphFieldContainerImpl extends AbstractGraphFieldContainerImpl implements NodeFieldContainer {

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
				createHTML(key).setHtml(htmlField.getHTML());
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
				NodeField nodeField = (NodeField) field;

				BootstrapInitializer.getBoot().nodeRoot().findByUuid(nodeField.getUuid(), rh -> {
					Node node = rh.result();

					// Check whether the container already contains a node field
					// TODO check node permissions
					com.gentics.mesh.core.data.node.field.nesting.GraphNodeField graphNodeField = getNode(key);
					if (graphNodeField == null) {
						createNode(key, node);
					} else {
						// We can't update the graphNodeField since it is in
						// fact an edge. We need to delete it and create a new
						// one.
						deleteField(key);
						createNode(key, node);
					}
				});
				break;
			case LIST:
				if (field instanceof NodeFieldListImpl) {
					NodeFieldListImpl nodeList = (NodeFieldListImpl) field;
					AtomicInteger integer = new AtomicInteger();
					GraphNodeFieldList graphNodeList = createNodeList(key);

					// Add the listed items
					for (NodeFieldListItem item : nodeList.getList()) {
						boot.nodeRoot().findByUuid(item.getUuid(), rh -> {
							if (rh.failed() || rh.result() == null) {
								// TODO log info that node was not found or
								// throw error? -> throw error
							} else {
								Node node = rh.result();
								graphNodeList.createNode(String.valueOf(integer.incrementAndGet()), node);
							}
						});
					}
				} else if (field instanceof StringFieldListImpl) {
					StringFieldListImpl stringList = (StringFieldListImpl) field;
					GraphStringFieldList graphStringList = createStringList(key);

					for (String item : stringList.getList()) {
						graphStringList.createString(item);
					}
				} else if (field instanceof HtmlFieldListImpl) {
					HtmlFieldListImpl htmlList = (HtmlFieldListImpl) field;
					GraphHtmlFieldList graphHtmlList = createHTMLList(key);

					for (String item : htmlList.getList()) {
						graphHtmlList.createHTML(item);
					}
				} else if (field instanceof NumberFieldListImpl) {
					NumberFieldListImpl numberList = (NumberFieldListImpl) field;
					GraphNumberFieldList graphNumberList = createNumberList(key);

					for (String item : numberList.getList()) {
						graphNumberList.createNumber(item);
					}
				} else if (field instanceof BooleanFieldListImpl) {

					BooleanFieldListImpl booleanList = (BooleanFieldListImpl) field;
					GraphBooleanFieldList graphBooleanList = createBooleanList(key);

					for (Boolean item : booleanList.getList()) {
						graphBooleanList.createBoolean(item);
					}
				} else if (field instanceof DateFieldListImpl) {
					DateFieldListImpl dateList = (DateFieldListImpl) field;
					GraphDateFieldList graphDateList = createDateList(key);

					for (String item : dateList.getList()) {
						graphDateList.createDate(item);
					}
				} else if (field instanceof MicroschemaFieldListImpl) {
					throw new NotImplementedException();
				} else {
					// TODO unknown type - throw better error
					throw new NotImplementedException();
				}
				break;
			case SELECT:
				SelectField restSelectField = (SelectFieldImpl) field;
				com.gentics.mesh.core.data.node.field.nesting.GraphSelectField<ListableGraphField> selectField = createSelect(key);
				// TODO impl
				throw new NotImplementedException();
				// break;
			case MICROSCHEMA:
				com.gentics.mesh.core.rest.node.field.MicroschemaField restMicroschemaField = (com.gentics.mesh.core.rest.node.field.impl.MicroschemaFieldImpl) field;
				GraphMicroschemaField microschemaField = createMicroschema(key);
				// TODO impl
				throw new NotImplementedException();
				// break;
			}

		}
		String extraFields = "";
		for (String key : fields.keySet()) {
			extraFields += "[" + key + "]";
		}
		if (!StringUtils.isEmpty(extraFields)) {
			throw new HttpStatusCodeErrorException(BAD_REQUEST, I18NService.getI18n().get(rc, "node_unhandled_fields", schema.getName(), extraFields));
			// throw new MeshSchemaException("The following fields were not
			// specified within the {" + schema.getName() + "} schema: " +
			// extraFields);
		}
	}

	private void deleteField(String key) {
		EdgeTraversal<?, ?, ?> traversal = outE(HAS_FIELD).has(com.gentics.mesh.core.data.node.field.GraphField.FIELD_KEY_PROPERTY_KEY, key);
		if (traversal.hasNext()) {
			traversal.next().remove();
		}
	}

	@Override
	public GraphMicroschemaField getMicroschema(String key) {
		throw new NotImplementedException();
	}

	@Override
	public GraphMicroschemaField createMicroschema(String key) {
		GraphMicroschemaFieldImpl field = getGraph().addFramedVertex(GraphMicroschemaFieldImpl.class);
		field.setFieldKey(key);
		linkOut(field, GraphRelationships.HAS_FIELD);
		return field;
	}

	@Override
	public Field getRestField(RoutingContext rc, String fieldKey, FieldSchema fieldSchema, boolean expandField) {
		FieldTypes type = FieldTypes.valueByName(fieldSchema.getType());
		// TODO replace switch case
		if (FieldTypes.STRING.equals(type)) {
			// TODO validate found fields has same type as schema
			com.gentics.mesh.core.data.node.field.basic.StringGraphField graphStringField = new com.gentics.mesh.core.data.node.field.impl.basic.StringGraphFieldImpl(fieldKey,
					this);
			// TODO handle null across all types

			StringFieldImpl stringField = new StringFieldImpl();
			String text = graphStringField.getString();
			stringField.setString(text == null ? "" : text);
			return stringField;
		}

		if (FieldTypes.NUMBER.equals(type)) {
			com.gentics.mesh.core.data.node.field.basic.NumberGraphField graphNumberField = getNumber(fieldKey);
			// TODO handle null across all types
			if (graphNumberField != null) {
				NumberFieldImpl numberField = new NumberFieldImpl();
				numberField.setNumber(graphNumberField.getNumber());
				return numberField;
			}
		}

		if (FieldTypes.DATE.equals(type)) {
			com.gentics.mesh.core.data.node.field.basic.DateGraphField graphDateField = getDate(fieldKey);
			// TODO handle null across all types
			if (graphDateField != null) {
				DateFieldImpl dateField = new DateFieldImpl();
				dateField.setDate(graphDateField.getDate());
				return dateField;
			}
		}

		if (FieldTypes.BOOLEAN.equals(type)) {
			com.gentics.mesh.core.data.node.field.basic.BooleanGraphField graphNumberField = getBoolean(fieldKey);
			// TODO handle null across all types
			if (graphNumberField != null) {
				BooleanFieldImpl booleanField = new BooleanFieldImpl();
				booleanField.setValue(graphNumberField.getBoolean());
				return booleanField;
			}
		}

		if (FieldTypes.NODE.equals(type)) {
			com.gentics.mesh.core.data.node.field.nesting.GraphNodeField graphNodeField = getNode(fieldKey);
			// TODO handle null across all types
			if (graphNodeField != null && graphNodeField.getNode() != null) {
				if (expandField) {
					// TODO don't use countdown latch here
					CountDownLatch latch = new CountDownLatch(1);
					AtomicReference<NodeResponse> reference = new AtomicReference<>();
					graphNodeField.getNode().transformToRest(rc, rh -> {
						reference.set(rh.result());
						latch.countDown();
					});
					try {
						latch.await(2, TimeUnit.SECONDS);
					} catch (Exception e) {
						e.printStackTrace();
					}
					return reference.get();
				} else {
					NodeFieldImpl nodeField = new NodeFieldImpl();
					nodeField.setUuid(graphNodeField.getNode().getUuid());
					return nodeField;
				}
			}
		}

		if (FieldTypes.HTML.equals(type)) {
			com.gentics.mesh.core.data.node.field.basic.HtmlGraphField graphStringField = new com.gentics.mesh.core.data.node.field.impl.basic.HtmlGraphFieldImpl(fieldKey, this);
			HtmlFieldImpl htmlField = new HtmlFieldImpl();
			String text = graphStringField.getHTML();
			htmlField.setHTML(text == null ? "" : text);
			return htmlField;
		}

		if (FieldTypes.LIST.equals(type)) {
			ListFieldSchema listFieldSchema = (ListFieldSchema) fieldSchema;

			switch (listFieldSchema.getListType()) {
			case GraphNodeFieldList.TYPE:
				GraphNodeFieldList nodeFieldList = getNodeList(fieldKey);
				NodeFieldListImpl restNodeFieldList = new NodeFieldListImpl();
				if (nodeFieldList == null) {
					return null;
				}
				for (com.gentics.mesh.core.data.node.field.nesting.GraphNodeField item : nodeFieldList.getList()) {
					if (expandField) {
						// TODO, FIXME get rid of the countdown latch
						CountDownLatch latch = new CountDownLatch(1);
						AtomicReference<NodeResponse> reference = new AtomicReference<>();
						item.getNode().transformToRest(rc, rh -> {
							reference.set(rh.result());
							latch.countDown();
						});
						try {
							latch.await(2, TimeUnit.SECONDS);
						} catch (Exception e) {
							e.printStackTrace();
						}
						restNodeFieldList.add(reference.get());
					} else {
						// Create the rest field and populate the fields
						NodeFieldListItemImpl listItem = new NodeFieldListItemImpl(item.getNode().getUuid());
						restNodeFieldList.add(listItem);
					}
				}
				return restNodeFieldList;
			case GraphNumberFieldList.TYPE:
				NumberFieldListImpl numberList = new NumberFieldListImpl();
				GraphNumberFieldList numberFieldList = getNumberList(fieldKey);
				if (numberFieldList == null) {
					return null;
				}
				for (com.gentics.mesh.core.data.node.field.basic.NumberGraphField item : numberFieldList.getList()) {
					numberList.add(item.getNumber());
				}
				return numberList;
			case GraphBooleanFieldList.TYPE:
				BooleanFieldListImpl booleanList = new BooleanFieldListImpl();
				GraphBooleanFieldList booleanFieldList = getBooleanList(fieldKey);
				if (booleanFieldList == null) {
					return null;
				}
				for (com.gentics.mesh.core.data.node.field.basic.BooleanGraphField item : booleanFieldList.getList()) {
					booleanList.add(item.getBoolean());
				}
				return booleanList;
			case GraphHtmlFieldList.TYPE:
				HtmlFieldListImpl htmlList = new HtmlFieldListImpl();
				GraphHtmlFieldList htmlFieldList = getHTMLList(fieldKey);
				if (htmlFieldList == null) {
					return null;
				}
				for (com.gentics.mesh.core.data.node.field.basic.HtmlGraphField item : htmlFieldList.getList()) {
					htmlList.add(item.getHTML());
				}
				return htmlList;
			case GraphMicroschemaFieldList.TYPE:
				MicroschemaFieldListImpl microschemaList = new MicroschemaFieldListImpl();
				return microschemaList;
			case GraphStringFieldList.TYPE:
				StringFieldListImpl stringList = new StringFieldListImpl();
				GraphStringFieldList stringFieldList = getStringList(fieldKey);

				if (stringFieldList == null) {
					return null;
				}

				for (com.gentics.mesh.core.data.node.field.basic.StringGraphField item : stringFieldList.getList()) {
					stringList.add(item.getString());
				}
				return stringList;

			case GraphDateFieldList.TYPE:
				DateFieldListImpl dateList = new DateFieldListImpl();
				GraphDateFieldList dateFieldList = getDateList(fieldKey);
				if (dateFieldList == null) {
					return null;
				}
				for (com.gentics.mesh.core.data.node.field.basic.DateGraphField item : dateFieldList.getList()) {
					dateList.add(item.getDate());
				}
				return dateList;
			}
			// String listType = listFielSchema.getListType();
		}
		if (FieldTypes.SELECT.equals(type)) {
			SelectFieldSchema selectFieldSchema = (SelectFieldSchema) fieldSchema;
			// throw new NotImplementedException();

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

	@Override
	public void delete() {
		// TODO delete linked aggregation nodes for node lists etc
		getElement().remove();
	}
}
