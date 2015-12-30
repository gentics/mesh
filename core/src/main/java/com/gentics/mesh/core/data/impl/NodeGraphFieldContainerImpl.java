package com.gentics.mesh.core.data.impl;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_FIELD;
import static com.gentics.mesh.core.rest.error.HttpStatusCodeErrorException.error;
import static com.gentics.mesh.core.rest.error.HttpStatusCodeErrorException.errorObservable;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang3.StringUtils;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.data.MicroschemaContainer;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.node.Micronode;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.field.BinaryGraphField;
import com.gentics.mesh.core.data.node.field.BooleanGraphField;
import com.gentics.mesh.core.data.node.field.DateGraphField;
import com.gentics.mesh.core.data.node.field.GraphField;
import com.gentics.mesh.core.data.node.field.HtmlGraphField;
import com.gentics.mesh.core.data.node.field.NumberGraphField;
import com.gentics.mesh.core.data.node.field.StringGraphField;
import com.gentics.mesh.core.data.node.field.list.BooleanGraphFieldList;
import com.gentics.mesh.core.data.node.field.list.DateGraphFieldList;
import com.gentics.mesh.core.data.node.field.list.HtmlGraphFieldList;
import com.gentics.mesh.core.data.node.field.list.MicronodeGraphFieldList;
import com.gentics.mesh.core.data.node.field.list.NodeGraphFieldList;
import com.gentics.mesh.core.data.node.field.list.NumberGraphFieldList;
import com.gentics.mesh.core.data.node.field.list.StringGraphFieldList;
import com.gentics.mesh.core.data.node.field.nesting.MicronodeGraphField;
import com.gentics.mesh.core.data.node.field.nesting.NodeGraphField;
import com.gentics.mesh.core.link.WebRootLinkReplacer;
import com.gentics.mesh.core.rest.common.FieldTypes;
import com.gentics.mesh.core.rest.error.HttpStatusCodeErrorException;
import com.gentics.mesh.core.rest.micronode.NullMicronodeResponse;
import com.gentics.mesh.core.rest.node.field.BinaryField;
import com.gentics.mesh.core.rest.node.field.BooleanField;
import com.gentics.mesh.core.rest.node.field.DateField;
import com.gentics.mesh.core.rest.node.field.Field;
import com.gentics.mesh.core.rest.node.field.HtmlField;
import com.gentics.mesh.core.rest.node.field.MicronodeField;
import com.gentics.mesh.core.rest.node.field.NodeField;
import com.gentics.mesh.core.rest.node.field.NodeFieldListItem;
import com.gentics.mesh.core.rest.node.field.NumberField;
import com.gentics.mesh.core.rest.node.field.StringField;
import com.gentics.mesh.core.rest.node.field.impl.BinaryFieldImpl;
import com.gentics.mesh.core.rest.node.field.impl.BooleanFieldImpl;
import com.gentics.mesh.core.rest.node.field.impl.DateFieldImpl;
import com.gentics.mesh.core.rest.node.field.impl.HtmlFieldImpl;
import com.gentics.mesh.core.rest.node.field.impl.NodeFieldImpl;
import com.gentics.mesh.core.rest.node.field.impl.NumberFieldImpl;
import com.gentics.mesh.core.rest.node.field.impl.StringFieldImpl;
import com.gentics.mesh.core.rest.node.field.list.MicronodeFieldList;
import com.gentics.mesh.core.rest.node.field.list.impl.BooleanFieldListImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.DateFieldListImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.HtmlFieldListImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.MicronodeFieldListImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.NodeFieldListImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.NumberFieldListImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.StringFieldListImpl;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.ListFieldSchema;
import com.gentics.mesh.core.rest.schema.MicronodeFieldSchema;
import com.gentics.mesh.core.rest.schema.MicroschemaReference;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.etc.MeshSpringConfiguration;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.handler.ActionContext;
import com.gentics.mesh.handler.InternalActionContext;
import com.syncleus.ferma.traversals.EdgeTraversal;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import rx.Observable;

public class NodeGraphFieldContainerImpl extends AbstractGraphFieldContainerImpl implements NodeGraphFieldContainer {

	private static final Logger log = LoggerFactory.getLogger(NodeGraphFieldContainerImpl.class);

	public static void checkIndices(Database database) {
		database.addVertexType(NodeGraphFieldContainerImpl.class);
	}

	private void failOnMissingMandatoryField(ActionContext ac, GraphField field, Field restField, FieldSchema fieldSchema, String key, Schema schema)
			throws HttpStatusCodeErrorException {
		if (field == null && fieldSchema.isRequired() && restField == null) {
			throw error(BAD_REQUEST, "node_error_missing_mandatory_field_value", key, schema.getName());
		}
	}

	@Override
	public String getDisplayFieldValue(Schema schema) {
		String displayFieldName = schema.getDisplayField();
		StringGraphField field = getString(displayFieldName);
		if (field != null) {
			return field.getString();
		}
		return null;
	}

	@Override
	public void updateFieldsFromRest(ActionContext ac, Map<String, Field> restFields, Schema schema) {

		BootstrapInitializer boot = BootstrapInitializer.getBoot();

		// Initially all fields are not yet handled
		List<String> unhandledFieldKeys = new ArrayList<>(restFields.size());
		unhandledFieldKeys.addAll(restFields.keySet());

		// Iterate over all known field that are listed in the schema for the node
		for (FieldSchema entry : schema.getFields()) {
			String key = entry.getName();
			Field restField = restFields.get(key);

			unhandledFieldKeys.remove(key);

			FieldTypes type = FieldTypes.valueByName(entry.getType());
			switch (type) {
			case HTML:
				HtmlGraphField htmlGraphField = getHtml(key);
				failOnMissingMandatoryField(ac, htmlGraphField, restField, entry, key, schema);
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
				failOnMissingMandatoryField(ac, graphStringField, restField, entry, key, schema);
				StringField stringField = (StringFieldImpl) restField;
				if (restField == null) {
					continue;
				}
				// Create new graph field if no existing one could be found
				if (graphStringField == null) {
					graphStringField = createString(key);
				}
				graphStringField.setString(stringField.getString());

				break;
			case BINARY:
				BinaryGraphField graphBinaryField = getBinary(key);
				failOnMissingMandatoryField(ac, graphBinaryField, restField, entry, key, schema);

				BinaryField binaryField = (BinaryFieldImpl) restField;
				if (restField == null) {
					continue;
				}
				// Create new graph field if no existing one could be found
				if (graphBinaryField == null) {
					graphBinaryField = createBinary(key);
				}
				graphBinaryField.setImageDPI(binaryField.getDpi());
				graphBinaryField.setFileName(binaryField.getFileName());
				graphBinaryField.setMimeType(binaryField.getMimeType());
				// Don't update image width, height, SHA checksum - those are immutable

				break;

			case NUMBER:
				NumberGraphField numberGraphField = getNumber(key);
				failOnMissingMandatoryField(ac, numberGraphField, restField, entry, key, schema);
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
				failOnMissingMandatoryField(ac, booleanGraphField, restField, entry, key, schema);
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
				failOnMissingMandatoryField(ac, dateGraphField, restField, entry, key, schema);
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
				NodeGraphField graphNodeField = getNode(key);
				failOnMissingMandatoryField(ac, graphNodeField, restField, entry, key, schema);
				NodeField nodeField = (NodeField) restField;
				if (restField == null) {
					continue;
				}
				Observable<Node> obsNode = boot.nodeRoot().findByUuid(nodeField.getUuid());
				obsNode.subscribe(node -> {
					if (node == null) {
						// TODO We want to delete the field when the field has been explicitly set to null
						if (log.isDebugEnabled()) {
							log.debug("Node field {" + key + "} could not be populated since node {" + nodeField.getUuid() + "} could not be found.");
						}
						// TODO we need to fail here - the node could not be found.
						// throw error(NOT_FOUND, "The field {, parameters)
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
				} , ac::fail);
				break;
			case LIST:
				if (restField instanceof NodeFieldListImpl) {
					NodeGraphFieldList graphNodeFieldList = getNodeList(key);
					failOnMissingMandatoryField(ac, graphNodeFieldList, restField, entry, key, schema);
					NodeFieldListImpl nodeList = (NodeFieldListImpl) restField;

					if (graphNodeFieldList == null) {
						graphNodeFieldList = createNodeList(key);
					} else {
						graphNodeFieldList.removeAll();
					}

					// Add the listed items
					AtomicInteger integer = new AtomicInteger();
					for (NodeFieldListItem item : nodeList.getItems()) {
						Node node = boot.nodeRoot().findByUuid(item.getUuid()).toBlocking().first();
						graphNodeFieldList.createNode(String.valueOf(integer.incrementAndGet()), node);
					}
				} else if (restField instanceof StringFieldListImpl) {
					StringGraphFieldList graphStringList = getStringList(key);
					failOnMissingMandatoryField(ac, graphStringList, restField, entry, key, schema);
					StringFieldListImpl stringList = (StringFieldListImpl) restField;

					if (graphStringList == null) {
						graphStringList = createStringList(key);
					} else {
						graphStringList.removeAll();
					}
					for (String item : stringList.getItems()) {
						graphStringList.createString(item);
					}

				} else if (restField instanceof HtmlFieldListImpl) {
					HtmlGraphFieldList graphHtmlFieldList = getHTMLList(key);
					failOnMissingMandatoryField(ac, graphHtmlFieldList, restField, entry, key, schema);
					HtmlFieldListImpl htmlList = (HtmlFieldListImpl) restField;

					if (graphHtmlFieldList == null) {
						graphHtmlFieldList = createHTMLList(key);
					} else {
						graphHtmlFieldList.removeAll();
					}
					for (String item : htmlList.getItems()) {
						graphHtmlFieldList.createHTML(item);
					}
				} else if (restField instanceof NumberFieldListImpl) {
					NumberGraphFieldList graphNumberFieldList = getNumberList(key);
					failOnMissingMandatoryField(ac, graphNumberFieldList, restField, entry, key, schema);
					NumberFieldListImpl numberList = (NumberFieldListImpl) restField;

					if (graphNumberFieldList == null) {
						graphNumberFieldList = createNumberList(key);
					} else {
						graphNumberFieldList.removeAll();
					}
					for (Number item : numberList.getItems()) {
						graphNumberFieldList.createNumber(item);
					}
				} else if (restField instanceof BooleanFieldListImpl) {
					BooleanGraphFieldList graphBooleanFieldList = getBooleanList(key);
					failOnMissingMandatoryField(ac, graphBooleanFieldList, restField, entry, key, schema);
					BooleanFieldListImpl booleanList = (BooleanFieldListImpl) restField;

					if (graphBooleanFieldList == null) {
						graphBooleanFieldList = createBooleanList(key);
					} else {
						graphBooleanFieldList.removeAll();
					}
					for (Boolean item : booleanList.getItems()) {
						graphBooleanFieldList.createBoolean(item);
					}
				} else if (restField instanceof DateFieldListImpl) {

					DateGraphFieldList graphDateFieldList = getDateList(key);
					failOnMissingMandatoryField(ac, graphDateFieldList, restField, entry, key, schema);
					DateFieldListImpl dateList = (DateFieldListImpl) restField;

					// Create new list if no existing one could be found
					if (graphDateFieldList == null) {
						graphDateFieldList = createDateList(key);
					} else {
						graphDateFieldList.removeAll();
					}
					for (Long item : dateList.getItems()) {
						graphDateFieldList.createDate(item);
					}
				} else if (restField instanceof MicronodeFieldListImpl) {
					MicronodeGraphFieldList micronodeGraphFieldList = getMicronodeList(key);
					failOnMissingMandatoryField(ac, micronodeGraphFieldList, restField, entry, key, schema);
					MicronodeFieldList micronodeList = (MicronodeFieldList) restField;

					if (micronodeGraphFieldList == null) {
						micronodeGraphFieldList = createMicronodeFieldList(key);
					}

					micronodeGraphFieldList.update(ac, micronodeList);

				} else {
					if (restField == null) {
						continue;
					} else {
						throw new NotImplementedException();
					}
				}
				break;
			case SELECT:
				// SelectField restSelectField = (SelectFieldImpl) restField;
				// com.gentics.mesh.core.data.node.field.nesting.SelectGraphField<ListableGraphField> selectField = createSelect(key);
				// TODO impl
				throw new NotImplementedException();
				// break;
			case MICRONODE:
				MicronodeFieldSchema microschemaFieldSchema = (MicronodeFieldSchema) entry;
				failOnMissingMandatoryField(ac, getMicronode(key), restField, entry, key, schema);
				if (restField == null) {
					continue;
				}
				MicronodeField micronodeField = (MicronodeField) restField;
				MicroschemaReference microschemaReference = micronodeField.getMicroschema();
				// TODO check for null
				if (microschemaReference == null) {
					continue;
				}
				String microschemaName = microschemaReference.getName();
				String microschemaUuid = microschemaReference.getUuid();
				MicroschemaContainer microschemaContainer = null;

				if (isEmpty(microschemaName) && isEmpty(microschemaUuid)) {
					//TODO i18n
					throw error(BAD_REQUEST, "No valid microschema reference could be found for field {" + key + "}");
				}
				// 1. Load microschema by uuid
				if (isEmpty(microschemaUuid)) {
					microschemaContainer = boot.microschemaContainerRoot().findByUuid(microschemaUuid).toBlocking().single();
					//					if (microschemaContainer == null) {
					//						throw error(BAD_REQUEST, "Could not find microschema for uuid  {" + microschemaUuid + "}");
					//					}
				}
				// 2. Load microschema by name
				if (microschemaContainer==null && !isEmpty(microschemaName)) {
					microschemaContainer = boot.microschemaContainerRoot().findByName(microschemaName).toBlocking().single();
					//					if (microschemaContainer == null) {
					//						//TODO i18n
					//						throw error(BAD_REQUEST, "Could not find microschema for name {" + microschemaName + "}");
					//					}
				}

				if (microschemaContainer == null) {
					//TODO i18n
					throw error(BAD_REQUEST, "Unable to update microschema field {" + key + "}. Could not find microschema by either name or uuid.");
				}

				Micronode micronode = null;
				MicronodeGraphField micronodeGraphField = getMicronode(key);

				// check whether microschema is allowed
				if (microschemaFieldSchema.getAllowedMicroSchemas() == null
						|| !Arrays.asList(microschemaFieldSchema.getAllowedMicroSchemas()).contains(microschemaContainer.getName())) {
					throw error(BAD_REQUEST, "node_error_invalid_microschema_field_value", key, microschemaContainer.getName());
				}

				// graphfield not set -> create one
				if (micronodeGraphField == null) {
					micronodeGraphField = createMicronode(key, microschemaContainer);
					micronode = micronodeGraphField.getMicronode();
				} else {
					// check whether uuid is equal
					micronode = micronodeGraphField.getMicronode();
					// TODO check whether micronode is null

					MicroschemaContainer existingContainer = micronode.getMicroschemaContainer();
					if ((!StringUtils.isEmpty(micronodeField.getUuid())
							&& !StringUtils.equalsIgnoreCase(micronode.getUuid(), micronodeField.getUuid()))
							|| !StringUtils.equalsIgnoreCase(microschemaContainer.getUuid(), existingContainer.getUuid())) {
						micronodeGraphField = createMicronode(key, microschemaContainer);
						micronode = micronodeGraphField.getMicronode();
					}
				}

				try {
					micronode.updateFieldsFromRest(ac, micronodeField.getFields());
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				break;
			}

		}

		// Some fields were specified within the json but were not specified in the schema. Those fields can't be handled. We throw an error to inform the user
		// about this.
		String extraFields = "";
		for (String key : unhandledFieldKeys)

		{
			extraFields += "[" + key + "]";
		}
		if (!StringUtils.isEmpty(extraFields))

		{
			throw error(BAD_REQUEST, "node_unhandled_fields", schema.getName(), extraFields);
		}

	}

	private void deleteField(String key) {
		EdgeTraversal<?, ?, ?> traversal = outE(HAS_FIELD).has(com.gentics.mesh.core.data.node.field.GraphField.FIELD_KEY_PROPERTY_KEY, key);
		if (traversal.hasNext()) {
			traversal.next().remove();
		}
	}

	@Override
	public Observable<? extends Field> getRestFieldFromGraph(InternalActionContext ac, String fieldKey, FieldSchema fieldSchema,
			boolean expandField) {

		// db.asyncNoTrx(noTrx -> {
		FieldTypes type = FieldTypes.valueByName(fieldSchema.getType());
		switch (type) {
		case STRING:
			// TODO validate found fields has same type as schema
			// StringGraphField graphStringField = new com.gentics.mesh.core.data.node.field.impl.basic.StringGraphFieldImpl(
			// fieldKey, this);
			StringGraphField graphStringField = getString(fieldKey);
			if (graphStringField == null) {
				return Observable.just(new StringFieldImpl());
			} else {
				return graphStringField.transformToRest(ac).map(stringField -> {
					if (ac.getResolveLinksType() != WebRootLinkReplacer.Type.OFF) {
						stringField.setString(WebRootLinkReplacer.getInstance().replace(stringField.getString(), ac.getResolveLinksType()));
					}
					return stringField;
				});
			}
		case BINARY:
			BinaryGraphField graphBinaryField = getBinary(fieldKey);
			if (graphBinaryField == null) {
				return Observable.just(new BinaryFieldImpl());
			} else {
				return graphBinaryField.transformToRest(ac);
			}
		case NUMBER:
			NumberGraphField graphNumberField = getNumber(fieldKey);
			if (graphNumberField == null) {
				return Observable.just(new NumberFieldImpl());
			} else {
				return graphNumberField.transformToRest(ac);
			}
		case DATE:
			DateGraphField graphDateField = getDate(fieldKey);
			if (graphDateField == null) {
				return Observable.just(new DateFieldImpl());
			} else {
				return graphDateField.transformToRest(ac);
			}
		case BOOLEAN:
			BooleanGraphField graphBooleanField = getBoolean(fieldKey);
			if (graphBooleanField == null) {
				return Observable.just(new BooleanFieldImpl());
			} else {
				return graphBooleanField.transformToRest(ac);
			}
		case NODE:
			NodeGraphField graphNodeField = getNode(fieldKey);
			if (graphNodeField == null) {
				return Observable.just(new NodeFieldImpl());
			} else {
				return graphNodeField.transformToRest(ac, fieldKey);
			}
		case HTML:
			HtmlGraphField graphHtmlField = getHtml(fieldKey);
			if (graphHtmlField == null) {
				return Observable.just(new HtmlFieldImpl());
			} else {
				return graphHtmlField.transformToRest(ac).map(model -> {
					// If needed resolve links within the html
					if (ac.getResolveLinksType() != WebRootLinkReplacer.Type.OFF) {
						model.setHTML(WebRootLinkReplacer.getInstance().replace(model.getHTML(), ac.getResolveLinksType()));
					}
					return model;
				});
			}
		case LIST:
			ListFieldSchema listFieldSchema = (ListFieldSchema) fieldSchema;

			switch (listFieldSchema.getListType()) {
			case NodeGraphFieldList.TYPE:
				NodeGraphFieldList nodeFieldList = getNodeList(fieldKey);
				if (nodeFieldList == null) {
					return Observable.just(new NodeFieldListImpl());
				} else {
					return nodeFieldList.transformToRest(ac, fieldKey);
				}
			case NumberGraphFieldList.TYPE:
				NumberGraphFieldList numberFieldList = getNumberList(fieldKey);
				if (numberFieldList == null) {
					return Observable.just(new NumberFieldListImpl());
				} else {
					return numberFieldList.transformToRest(ac, fieldKey);
				}
			case BooleanGraphFieldList.TYPE:
				BooleanGraphFieldList booleanFieldList = getBooleanList(fieldKey);
				if (booleanFieldList == null) {
					return Observable.just(new BooleanFieldListImpl());
				} else {
					return booleanFieldList.transformToRest(ac, fieldKey);
				}
			case HtmlGraphFieldList.TYPE:
				HtmlGraphFieldList htmlFieldList = getHTMLList(fieldKey);
				if (htmlFieldList == null) {
					return Observable.just(new HtmlFieldListImpl());
				} else {
					return htmlFieldList.transformToRest(ac, fieldKey);
				}
			case MicronodeGraphFieldList.TYPE:
				MicronodeGraphFieldList graphMicroschemaField = getMicronodeList(fieldKey);
				if (graphMicroschemaField == null) {
					return Observable.just(new MicronodeFieldListImpl());
				} else {
					return graphMicroschemaField.transformToRest(ac, fieldKey);
				}
			case StringGraphFieldList.TYPE:
				StringGraphFieldList stringFieldList = getStringList(fieldKey);
				if (stringFieldList == null) {
					return Observable.just(new StringFieldListImpl());
				} else {
					return stringFieldList.transformToRest(ac, fieldKey);
				}
			case DateGraphFieldList.TYPE:
				DateGraphFieldList dateFieldList = getDateList(fieldKey);
				if (dateFieldList == null) {
					return Observable.just(new DateFieldListImpl());
				} else {
					return dateFieldList.transformToRest(ac, fieldKey);
				}
			}
			// String listType = listFielSchema.getListType();
			break;
		case SELECT:
			// GraphSelectField graphSelectField = getSelect(fieldKey);
			// if (graphSelectField == null) {
			// return new SelectFieldImpl();
			// } else {
			// //TODO impl me
			// //graphSelectField.transformToRest(ac);
			// }
			// throw new NotImplementedException();
			break;
		case MICRONODE:
			MicronodeGraphField micronodeGraphField = getMicronode(fieldKey);
			if (micronodeGraphField == null) {
				return Observable.just(new NullMicronodeResponse());
			} else {
				return micronodeGraphField.transformToRest(ac, fieldKey);
			}
		}

		return errorObservable(BAD_REQUEST, "type unknown");

	}

	@Override
	public void delete() {
		// TODO delete linked aggregation nodes for node lists etc
		getElement().remove();
	}

}
