package com.gentics.mesh.core.data.impl;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_FIELD;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang3.StringUtils;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.field.GraphField;
import com.gentics.mesh.core.data.node.field.basic.BooleanGraphField;
import com.gentics.mesh.core.data.node.field.basic.DateGraphField;
import com.gentics.mesh.core.data.node.field.basic.HtmlGraphField;
import com.gentics.mesh.core.data.node.field.basic.NumberGraphField;
import com.gentics.mesh.core.data.node.field.basic.StringGraphField;
import com.gentics.mesh.core.data.node.field.impl.nesting.GraphMicroschemaFieldImpl;
import com.gentics.mesh.core.data.node.field.list.GraphBooleanFieldList;
import com.gentics.mesh.core.data.node.field.list.GraphDateFieldList;
import com.gentics.mesh.core.data.node.field.list.GraphHtmlFieldList;
import com.gentics.mesh.core.data.node.field.list.GraphMicroschemaFieldList;
import com.gentics.mesh.core.data.node.field.list.GraphNodeFieldList;
import com.gentics.mesh.core.data.node.field.list.GraphNumberFieldList;
import com.gentics.mesh.core.data.node.field.list.GraphStringFieldList;
import com.gentics.mesh.core.data.node.field.nesting.GraphMicroschemaField;
import com.gentics.mesh.core.data.node.field.nesting.GraphNodeField;
import com.gentics.mesh.core.data.node.field.nesting.ListableGraphField;
import com.gentics.mesh.core.data.relationship.GraphRelationships;
import com.gentics.mesh.core.rest.common.FieldTypes;
import com.gentics.mesh.core.rest.error.HttpStatusCodeErrorException;
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
import com.gentics.mesh.core.rest.node.field.impl.MicroschemaFieldImpl;
import com.gentics.mesh.core.rest.node.field.impl.NodeFieldImpl;
import com.gentics.mesh.core.rest.node.field.impl.NumberFieldImpl;
import com.gentics.mesh.core.rest.node.field.impl.SelectFieldImpl;
import com.gentics.mesh.core.rest.node.field.impl.StringFieldImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.BooleanFieldListImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.DateFieldListImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.HtmlFieldListImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.MicroschemaFieldListImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.NodeFieldListImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.NumberFieldListImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.StringFieldListImpl;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.ListFieldSchema;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.error.MeshSchemaException;
import com.gentics.mesh.handler.ActionContext;
import com.syncleus.ferma.traversals.EdgeTraversal;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class NodeGraphFieldContainerImpl extends AbstractGraphFieldContainerImpl implements NodeGraphFieldContainer {

	private static final Logger log = LoggerFactory.getLogger(NodeGraphFieldContainerImpl.class);

	private void failOnMissingMandatoryField(GraphField field, Field restField, FieldSchema schema, String key) throws MeshSchemaException {
		if (field == null && schema.isRequired() && restField == null) {
			throw new MeshSchemaException("Could not find value for required schema field with key {" + key + "}");
		}
	}

	@Override
	public void setFieldFromRest(ActionContext ac, Map<String, Field> restFields, Schema schema) throws MeshSchemaException {

		BootstrapInitializer boot = BootstrapInitializer.getBoot();

		// Iterate over all known field that are listed in the schema for the node
		for (FieldSchema entry : schema.getFields()) {
			String key = entry.getName();
			Field restField = restFields.get(key);
			restFields.remove(key);

			FieldTypes type = FieldTypes.valueByName(entry.getType());
			switch (type) {
			case HTML:
				HtmlGraphField htmlGraphField = getHtml(key);
				failOnMissingMandatoryField(htmlGraphField, restField, entry, key);
				HtmlField htmlField = (HtmlFieldImpl) restField;
				if (restField == null) {
					continue;
				}

				// Create new graph field if no existing one could be found
				if (htmlGraphField == null) {
					createHTML(key).setHtml(htmlField.getHTML());
				} else {
					htmlGraphField.setHtml(htmlField.getHTML());
				}
				break;
			case STRING:
				StringGraphField graphStringField = getString(key);
				failOnMissingMandatoryField(graphStringField, restField, entry, key);
				StringField stringField = (StringFieldImpl) restField;
				if (restField == null) {
					continue;
				}
				// Create new graph field if no existing one could be found
				if (graphStringField == null) {
					createString(key).setString(stringField.getString());
				} else {
					graphStringField.setString(stringField.getString());
				}
				break;
			case NUMBER:
				NumberGraphField numberGraphField = getNumber(key);
				failOnMissingMandatoryField(numberGraphField, restField, entry, key);
				NumberField numberField = (NumberFieldImpl) restField;
				if (restField == null) {
					continue;
				}
				if (numberGraphField == null) {
					createNumber(key).setNumber(numberField.getNumber());
				} else {
					numberGraphField.setNumber(numberField.getNumber());
				}
				break;
			case BOOLEAN:
				BooleanGraphField booleanGraphField = getBoolean(key);
				failOnMissingMandatoryField(booleanGraphField, restField, entry, key);
				BooleanField booleanField = (BooleanFieldImpl) restField;
				if (restField == null) {
					continue;
				}
				if (booleanGraphField == null) {
					createBoolean(key).setBoolean(booleanField.getValue());
				} else {
					booleanGraphField.setBoolean(booleanField.getValue());
				}
				break;
			case DATE:
				DateGraphField dateGraphField = getDate(key);
				failOnMissingMandatoryField(dateGraphField, restField, entry, key);
				DateField dateField = (DateFieldImpl) restField;
				if (restField == null) {
					continue;
				}
				if (dateGraphField == null) {
					createDate(key).setDate(dateField.getDate());
				} else {
					dateGraphField.setDate(dateField.getDate());
				}
				break;
			case NODE:
				GraphNodeField graphNodeField = getNode(key);
				failOnMissingMandatoryField(graphNodeField, restField, entry, key);
				NodeField nodeField = (NodeField) restField;
				if (restField == null) {
					continue;
				}
				BootstrapInitializer.getBoot().nodeRoot().findByUuid(nodeField.getUuid(), rh -> {
					Node node = rh.result();
					if (node == null) {
						log.error("Node field {" + key + "} could not be populated since node {" + nodeField.getUuid() + "} could not be found.");
						//TODO we need to fail here - the node could not be found.
						//throw new HttpStatusCodeErrorException(NOT_FOUND, ac.i18n("The field {, parameters))
					} else {
						// Check whether the container already contains a node field
						// TODO check node permissions
						if (graphNodeField == null) {
							createNode(key, node);
						} else {
							// We can't update the graphNodeField since it is in
							// fact an edge. We need to delete it and create a new
							// one.
							deleteField(key);
							createNode(key, node);
						}
					}
				});
				break;
			case LIST:

				if (restField instanceof NodeFieldListImpl) {
					GraphNodeFieldList graphNodeFieldList = getNodeList(key);
					failOnMissingMandatoryField(graphNodeFieldList, restField, entry, key);
					NodeFieldListImpl nodeList = (NodeFieldListImpl) restField;

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
				} else if (restField instanceof StringFieldListImpl) {
					GraphStringFieldList graphStringList = getStringList(key);
					failOnMissingMandatoryField(graphStringList, restField, entry, key);
					StringFieldListImpl stringList = (StringFieldListImpl) restField;

					if (graphStringList == null) {
						graphStringList = createStringList(key);
						for (String item : stringList.getList()) {
							graphStringList.createString(item);
						}
					} else {
						//TODO handle update - remove all strings and set the new ones

					}

				} else if (restField instanceof HtmlFieldListImpl) {
					GraphHtmlFieldList graphHtmlFieldList = getHTMLList(key);
					failOnMissingMandatoryField(graphHtmlFieldList, restField, entry, key);
					HtmlFieldListImpl htmlList = (HtmlFieldListImpl) restField;

					if (graphHtmlFieldList == null) {
						GraphHtmlFieldList graphHtmlList = createHTMLList(key);
						for (String item : htmlList.getList()) {
							graphHtmlList.createHTML(item);
						}
					} else {
						//TODO handle update
					}
				} else if (restField instanceof NumberFieldListImpl) {
					GraphNumberFieldList graphNumberFieldList = getNumberList(key);
					failOnMissingMandatoryField(graphNumberFieldList, restField, entry, key);
					NumberFieldListImpl numberList = (NumberFieldListImpl) restField;

					if (graphNumberFieldList == null) {
						GraphNumberFieldList graphNumberList = createNumberList(key);
						for (String item : numberList.getList()) {
							graphNumberList.createNumber(item);
						}
					} else {
						//TODO handle update
					}
				} else if (restField instanceof BooleanFieldListImpl) {
					GraphBooleanFieldList graphBooleanFieldList = getBooleanList(key);
					failOnMissingMandatoryField(graphBooleanFieldList, restField, entry, key);
					BooleanFieldListImpl booleanList = (BooleanFieldListImpl) restField;

					if (graphBooleanFieldList == null) {
						GraphBooleanFieldList graphBooleanList = createBooleanList(key);
						for (Boolean item : booleanList.getList()) {
							graphBooleanList.createBoolean(item);
						}
					} else {
						//TODO handle update
					}
				} else if (restField instanceof DateFieldListImpl) {

					GraphDateFieldList graphDateFieldList = getDateList(key);
					failOnMissingMandatoryField(graphDateFieldList, restField, entry, key);
					DateFieldListImpl dateList = (DateFieldListImpl) restField;

					// Create new list if no existing one could be found
					if (graphDateFieldList == null) {
						graphDateFieldList = createDateList(key);
						for (String item : dateList.getList()) {
							graphDateFieldList.createDate(item);
						}
					} else {
						//TODO handle Update
					}
				} else if (restField instanceof MicroschemaFieldListImpl) {
					throw new NotImplementedException();
				} else {
					if (restField == null) {
						continue;
					} else {
						throw new NotImplementedException();
					}
				}
				break;
			case SELECT:
				SelectField restSelectField = (SelectFieldImpl) restField;
				com.gentics.mesh.core.data.node.field.nesting.GraphSelectField<ListableGraphField> selectField = createSelect(key);
				// TODO impl
				throw new NotImplementedException();
				// break;
			case MICROSCHEMA:
				com.gentics.mesh.core.rest.node.field.MicroschemaField restMicroschemaField = (com.gentics.mesh.core.rest.node.field.impl.MicroschemaFieldImpl) restField;
				GraphMicroschemaField microschemaField = createMicroschema(key);
				// TODO impl
				throw new NotImplementedException();
				// break;
			}

		}
		String extraFields = "";
		for (String key : restFields.keySet()) {
			extraFields += "[" + key + "]";
		}
		if (!StringUtils.isEmpty(extraFields)) {
			throw new HttpStatusCodeErrorException(BAD_REQUEST, ac.i18n("node_unhandled_fields", schema.getName(), extraFields));
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
	public Field getRestFieldFromGraph(ActionContext ac, String fieldKey, FieldSchema fieldSchema, boolean expandField) {
		FieldTypes type = FieldTypes.valueByName(fieldSchema.getType());
		switch (type) {
		case STRING:
			// TODO validate found fields has same type as schema
			//			StringGraphField graphStringField = new com.gentics.mesh.core.data.node.field.impl.basic.StringGraphFieldImpl(
			//					fieldKey, this);
			StringGraphField graphStringField = getString(fieldKey);
			if (graphStringField == null) {
				return new StringFieldImpl();
			} else {
				return graphStringField.transformToRest(ac);
			}
		case NUMBER:
			NumberGraphField graphNumberField = getNumber(fieldKey);
			if (graphNumberField == null) {
				return new NumberFieldImpl();
			} else {
				return graphNumberField.transformToRest(ac);
			}

		case DATE:
			DateGraphField graphDateField = getDate(fieldKey);
			if (graphDateField == null) {
				return new DateFieldImpl();
			} else {
				return graphDateField.transformToRest(ac);
			}
		case BOOLEAN:
			BooleanGraphField graphBooleanField = getBoolean(fieldKey);
			if (graphBooleanField == null) {
				return new BooleanFieldImpl();
			} else {
				return graphBooleanField.transformToRest(ac);
			}
		case NODE:
			GraphNodeField graphNodeField = getNode(fieldKey);
			if (graphNodeField == null) {
				return new NodeFieldImpl();
			} else {
				return graphNodeField.transformToRest(ac, fieldKey);
			}
		case HTML:
			HtmlGraphField graphHtmlField = getHtml(fieldKey);
			if (graphHtmlField == null) {
				return new HtmlFieldImpl();
			} else {
				return graphHtmlField.transformToRest(ac);
			}
		case LIST:
			ListFieldSchema listFieldSchema = (ListFieldSchema) fieldSchema;

			switch (listFieldSchema.getListType()) {
			case GraphNodeFieldList.TYPE:
				GraphNodeFieldList nodeFieldList = getNodeList(fieldKey);
				if (nodeFieldList == null) {
					return new NodeFieldListImpl();
				} else {
					return nodeFieldList.transformToRest(ac, fieldKey);
				}
			case GraphNumberFieldList.TYPE:
				GraphNumberFieldList numberFieldList = getNumberList(fieldKey);
				if (numberFieldList == null) {
					return new NumberFieldListImpl();
				} else {
					return numberFieldList.transformToRest(ac, fieldKey);
				}
			case GraphBooleanFieldList.TYPE:
				GraphBooleanFieldList booleanFieldList = getBooleanList(fieldKey);
				if (booleanFieldList == null) {
					return new BooleanFieldListImpl();
				} else {
					return booleanFieldList.transformToRest(ac, fieldKey);
				}
			case GraphHtmlFieldList.TYPE:
				GraphHtmlFieldList htmlFieldList = getHTMLList(fieldKey);
				if (htmlFieldList == null) {
					return new HtmlFieldListImpl();
				} else {
					return htmlFieldList.transformToRest(ac, fieldKey);
				}
			case GraphMicroschemaFieldList.TYPE:
				GraphMicroschemaFieldList graphMicroschemaField = getMicroschemaList(fieldKey);
				if (graphMicroschemaField == null) {
					return new MicroschemaFieldListImpl();
				} else {
					return graphMicroschemaField.transformToRest(ac, fieldKey);
				}
			case GraphStringFieldList.TYPE:
				GraphStringFieldList stringFieldList = getStringList(fieldKey);
				if (stringFieldList == null) {
					return new StringFieldListImpl();
				} else {
					return stringFieldList.transformToRest(ac, fieldKey);
				}
			case GraphDateFieldList.TYPE:
				GraphDateFieldList dateFieldList = getDateList(fieldKey);
				if (dateFieldList == null) {
					return new DateFieldListImpl();
				} else {
					return dateFieldList.transformToRest(ac, fieldKey);
				}
			}
			// String listType = listFielSchema.getListType();
			break;
		case SELECT:
			//			GraphSelectField graphSelectField = getSelect(fieldKey);
			//			if (graphSelectField == null) {
			//				return new SelectFieldImpl();
			//			} else {
			//				//TODO impl me
			//				//graphSelectField.transformToRest(ac);
			//			}
			// throw new NotImplementedException();
			break;
		case MICROSCHEMA:
			GraphMicroschemaField graphMicroschemaField = getMicroschema(fieldKey);
			if (graphMicroschemaField == null) {
				return new MicroschemaFieldImpl();
			} else {
				//TODO impl me
				//graphMicroschemaField.transformToRest(ac);
				throw new NotImplementedException();
			}
		}

		return null;
	}

	@Override
	public void delete() {
		// TODO delete linked aggregation nodes for node lists etc
		getElement().remove();
	}
}
