package com.gentics.mesh.core.data.container.impl;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_FIELD;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_LIST;
import static com.gentics.mesh.core.rest.error.Errors.error;
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
import com.gentics.mesh.core.data.GraphFieldContainer;
import com.gentics.mesh.core.data.MicroschemaContainer;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.generic.MeshEdgeImpl;
import com.gentics.mesh.core.data.node.Micronode;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.field.BinaryGraphField;
import com.gentics.mesh.core.data.node.field.BooleanGraphField;
import com.gentics.mesh.core.data.node.field.DateGraphField;
import com.gentics.mesh.core.data.node.field.GraphField;
import com.gentics.mesh.core.data.node.field.HtmlGraphField;
import com.gentics.mesh.core.data.node.field.NumberGraphField;
import com.gentics.mesh.core.data.node.field.StringGraphField;
import com.gentics.mesh.core.data.node.field.impl.BinaryGraphFieldImpl;
import com.gentics.mesh.core.data.node.field.impl.BooleanGraphFieldImpl;
import com.gentics.mesh.core.data.node.field.impl.DateGraphFieldImpl;
import com.gentics.mesh.core.data.node.field.impl.HtmlGraphFieldImpl;
import com.gentics.mesh.core.data.node.field.impl.MicronodeGraphFieldImpl;
import com.gentics.mesh.core.data.node.field.impl.NodeGraphFieldImpl;
import com.gentics.mesh.core.data.node.field.impl.NumberGraphFieldImpl;
import com.gentics.mesh.core.data.node.field.impl.StringGraphFieldImpl;
import com.gentics.mesh.core.data.node.field.list.BooleanGraphFieldList;
import com.gentics.mesh.core.data.node.field.list.DateGraphFieldList;
import com.gentics.mesh.core.data.node.field.list.HtmlGraphFieldList;
import com.gentics.mesh.core.data.node.field.list.ListGraphField;
import com.gentics.mesh.core.data.node.field.list.MicronodeGraphFieldList;
import com.gentics.mesh.core.data.node.field.list.NodeGraphFieldList;
import com.gentics.mesh.core.data.node.field.list.NumberGraphFieldList;
import com.gentics.mesh.core.data.node.field.list.StringGraphFieldList;
import com.gentics.mesh.core.data.node.field.list.impl.BooleanGraphFieldListImpl;
import com.gentics.mesh.core.data.node.field.list.impl.DateGraphFieldListImpl;
import com.gentics.mesh.core.data.node.field.list.impl.HtmlGraphFieldListImpl;
import com.gentics.mesh.core.data.node.field.list.impl.MicronodeGraphFieldListImpl;
import com.gentics.mesh.core.data.node.field.list.impl.NodeGraphFieldListImpl;
import com.gentics.mesh.core.data.node.field.list.impl.NumberGraphFieldListImpl;
import com.gentics.mesh.core.data.node.field.list.impl.StringGraphFieldListImpl;
import com.gentics.mesh.core.data.node.field.nesting.MicronodeGraphField;
import com.gentics.mesh.core.data.node.field.nesting.NodeGraphField;
import com.gentics.mesh.core.data.node.impl.MicronodeImpl;
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
import com.gentics.mesh.core.rest.schema.FieldSchemaContainer;
import com.gentics.mesh.core.rest.schema.ListFieldSchema;
import com.gentics.mesh.core.rest.schema.MicronodeFieldSchema;
import com.gentics.mesh.core.rest.schema.MicroschemaReference;
import com.gentics.mesh.core.rest.schema.StringFieldSchema;
import com.gentics.mesh.handler.InternalActionContext;
import com.syncleus.ferma.traversals.EdgeTraversal;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import rx.Observable;

/**
 * Abstract implementation for a field container. A {@link GraphFieldContainer} is used to store {@link GraphField} instances.
 */
public abstract class AbstractGraphFieldContainerImpl extends AbstractBasicGraphFieldContainerImpl implements GraphFieldContainer {

	private static final Logger log = LoggerFactory.getLogger(AbstractGraphFieldContainerImpl.class);

	/**
	 * Return the parent node of the field container.
	 * 
	 * @return
	 */
	abstract protected Node getParentNode();

	@Override
	public StringGraphField createString(String key) {
		// TODO check whether the key is already occupied
		StringGraphFieldImpl field = new StringGraphFieldImpl(key, this);
		field.setFieldKey(key);
		return field;
	}

	@Override
	public StringGraphField getString(String key) {
		if (fieldExists(key, "string")) {
			return new StringGraphFieldImpl(key, this);
		}
		return null;
	}

	@Override
	public NodeGraphField createNode(String key, Node node) {
		NodeGraphFieldImpl field = getGraph().addFramedEdge(this, node.getImpl(), HAS_FIELD, NodeGraphFieldImpl.class);
		field.setFieldKey(key);
		return field;
	}

	@Override
	public NodeGraphField getNode(String key) {
		return outE(HAS_FIELD).has(NodeGraphFieldImpl.class).has(GraphField.FIELD_KEY_PROPERTY_KEY, key)
				.nextOrDefaultExplicit(NodeGraphFieldImpl.class, null);
	}

	@Override
	public DateGraphField createDate(String key) {
		DateGraphFieldImpl field = new DateGraphFieldImpl(key, this);
		field.setFieldKey(key);
		return field;
	}

	@Override
	public DateGraphField getDate(String key) {
		if (fieldExists(key, "date")) {
			return new DateGraphFieldImpl(key, this);
		}
		return null;
	}

	@Override
	public NumberGraphField createNumber(String key) {
		NumberGraphFieldImpl field = new NumberGraphFieldImpl(key, this);
		field.setFieldKey(key);
		return field;
	}

	@Override
	public NumberGraphField getNumber(String key) {
		if (fieldExists(key, "number")) {
			return new NumberGraphFieldImpl(key, this);
		}
		return null;
	}

	@Override
	public HtmlGraphField createHTML(String key) {
		HtmlGraphFieldImpl field = new HtmlGraphFieldImpl(key, this);
		field.setFieldKey(key);
		return field;
	}

	@Override
	public HtmlGraphField getHtml(String key) {
		if (fieldExists(key, "html")) {
			return new HtmlGraphFieldImpl(key, this);
		}
		return null;
	}

	@Override
	public BooleanGraphField createBoolean(String key) {
		BooleanGraphFieldImpl field = new BooleanGraphFieldImpl(key, this);
		field.setFieldKey(key);
		return field;
	}

	@Override
	public BooleanGraphField getBoolean(String key) {
		if (fieldExists(key, "boolean")) {
			return new BooleanGraphFieldImpl(key, this);
		}
		return null;
	}

	@Override
	public MicronodeGraphField createMicronode(String key, MicroschemaContainer microschema) {
		// delete existing micronode
		MicronodeGraphField existing = getMicronode(key);
		if (existing != null) {
			existing.getMicronode().delete();
		}

		MicronodeImpl micronode = getGraph().addFramedVertex(MicronodeImpl.class);
		micronode.setMicroschemaContainer(microschema);
		MicronodeGraphField field = getGraph().addFramedEdge(this, micronode, HAS_FIELD, MicronodeGraphFieldImpl.class);
		field.setFieldKey(key);
		return field;
	}

	@Override
	public MicronodeGraphField getMicronode(String key) {
		return outE(HAS_FIELD).has(MicronodeGraphFieldImpl.class).has(GraphField.FIELD_KEY_PROPERTY_KEY, key)
				.nextOrDefaultExplicit(MicronodeGraphFieldImpl.class, null);
	}

	@Override
	public BinaryGraphField createBinary(String key) {
		BinaryGraphFieldImpl field = getGraph().addFramedVertex(BinaryGraphFieldImpl.class);
		field.setFieldKey(key);

		MeshEdgeImpl edge = getGraph().addFramedEdge(this, field, HAS_FIELD, MeshEdgeImpl.class);
		edge.setProperty(GraphField.FIELD_KEY_PROPERTY_KEY, key);
		return field;
	}

	@Override
	public BinaryGraphField getBinary(String key) {
		return out(HAS_FIELD).has(BinaryGraphFieldImpl.class).has(GraphField.FIELD_KEY_PROPERTY_KEY, key)
				.nextOrDefaultExplicit(BinaryGraphFieldImpl.class, null);
	}

	@Override
	public NumberGraphFieldList createNumberList(String fieldKey) {
		return createList(NumberGraphFieldListImpl.class, fieldKey);
	}

	@Override
	public NumberGraphFieldList getNumberList(String fieldKey) {
		return getList(NumberGraphFieldListImpl.class, fieldKey);
	}

	@Override
	public NodeGraphFieldList createNodeList(String fieldKey) {
		return createList(NodeGraphFieldListImpl.class, fieldKey);
	}

	@Override
	public NodeGraphFieldList getNodeList(String fieldKey) {
		return getList(NodeGraphFieldListImpl.class, fieldKey);
	}

	@Override
	public StringGraphFieldList createStringList(String fieldKey) {
		return createList(StringGraphFieldListImpl.class, fieldKey);
	}

	@Override
	public StringGraphFieldList getStringList(String fieldKey) {
		return getList(StringGraphFieldListImpl.class, fieldKey);
	}

	@Override
	public BooleanGraphFieldList createBooleanList(String fieldKey) {
		return createList(BooleanGraphFieldListImpl.class, fieldKey);
	}

	@Override
	public BooleanGraphFieldList getBooleanList(String fieldKey) {
		return getList(BooleanGraphFieldListImpl.class, fieldKey);
	}

	@Override
	public MicronodeGraphFieldList createMicronodeFieldList(String fieldKey) {
		return createList(MicronodeGraphFieldListImpl.class, fieldKey);
	}

	@Override
	public MicronodeGraphFieldList getMicronodeList(String fieldKey) {
		return getList(MicronodeGraphFieldListImpl.class, fieldKey);
	}

	@Override
	public HtmlGraphFieldList createHTMLList(String fieldKey) {
		return createList(HtmlGraphFieldListImpl.class, fieldKey);
	}

	@Override
	public HtmlGraphFieldList getHTMLList(String fieldKey) {
		return getList(HtmlGraphFieldListImpl.class, fieldKey);
	}

	@Override
	public DateGraphFieldList createDateList(String fieldKey) {
		return createList(DateGraphFieldListImpl.class, fieldKey);
	}

	@Override
	public DateGraphFieldList getDateList(String fieldKey) {
		return getList(DateGraphFieldListImpl.class, fieldKey);
	}

	private <T extends ListGraphField<?, ?, ?>> T getList(Class<T> classOfT, String fieldKey) {
		return out(HAS_LIST).has(classOfT).has(GraphField.FIELD_KEY_PROPERTY_KEY, fieldKey).nextOrDefaultExplicit(classOfT, null);
	}

	/**
	 * Create new list of the given type.
	 * 
	 * @param classOfT
	 *            Implementation/Type of list
	 * @param fieldKey
	 *            Field key for the list
	 * @return
	 */
	private <T extends ListGraphField<?, ?, ?>> T createList(Class<T> classOfT, String fieldKey) {
		T list = getGraph().addFramedVertex(classOfT);
		list.setFieldKey(fieldKey);
		linkOut(list.getImpl(), HAS_LIST);
		return list;
	}

	@Override
	public Observable<? extends Field> getRestFieldFromGraph(InternalActionContext ac, String fieldKey, FieldSchema fieldSchema,
			List<String> languageTags) {

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
						Project project = ac.getProject();
						if (project == null) {
							project = getParentNode().getProject();
						}
						stringField.setString(WebRootLinkReplacer.getInstance().replace(stringField.getString(),
								ac.getResolveLinksType(), project.getName(), languageTags));
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
				return graphNodeField.transformToRest(ac, fieldKey, languageTags);
			}
		case HTML:
			HtmlGraphField graphHtmlField = getHtml(fieldKey);
			if (graphHtmlField == null) {
				return Observable.just(new HtmlFieldImpl());
			} else {
				return graphHtmlField.transformToRest(ac).map(model -> {
					// If needed resolve links within the html
					if (ac.getResolveLinksType() != WebRootLinkReplacer.Type.OFF) {
						Project project = ac.getProject();
						if (project == null) {
							project = getParentNode().getProject();
						}
						model.setHTML(WebRootLinkReplacer.getInstance().replace(model.getHTML(),
								ac.getResolveLinksType(), project.getName(), languageTags));
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
					return nodeFieldList.transformToRest(ac, fieldKey, languageTags);
				}
			case NumberGraphFieldList.TYPE:
				NumberGraphFieldList numberFieldList = getNumberList(fieldKey);
				if (numberFieldList == null) {
					return Observable.just(new NumberFieldListImpl());
				} else {
					return numberFieldList.transformToRest(ac, fieldKey, languageTags);
				}
			case BooleanGraphFieldList.TYPE:
				BooleanGraphFieldList booleanFieldList = getBooleanList(fieldKey);
				if (booleanFieldList == null) {
					return Observable.just(new BooleanFieldListImpl());
				} else {
					return booleanFieldList.transformToRest(ac, fieldKey, languageTags);
				}
			case HtmlGraphFieldList.TYPE:
				HtmlGraphFieldList htmlFieldList = getHTMLList(fieldKey);
				if (htmlFieldList == null) {
					return Observable.just(new HtmlFieldListImpl());
				} else {
					return htmlFieldList.transformToRest(ac, fieldKey, languageTags);
				}
			case MicronodeGraphFieldList.TYPE:
				MicronodeGraphFieldList graphMicroschemaField = getMicronodeList(fieldKey);
				if (graphMicroschemaField == null) {
					return Observable.just(new MicronodeFieldListImpl());
				} else {
					return graphMicroschemaField.transformToRest(ac, fieldKey, languageTags);
				}
			case StringGraphFieldList.TYPE:
				StringGraphFieldList stringFieldList = getStringList(fieldKey);
				if (stringFieldList == null) {
					return Observable.just(new StringFieldListImpl());
				} else {
					return stringFieldList.transformToRest(ac, fieldKey, languageTags);
				}
			case DateGraphFieldList.TYPE:
				DateGraphFieldList dateFieldList = getDateList(fieldKey);
				if (dateFieldList == null) {
					return Observable.just(new DateFieldListImpl());
				} else {
					return dateFieldList.transformToRest(ac, fieldKey, languageTags);
				}
			}
			// String listType = listFielSchema.getListType();
			break;
		case MICRONODE:
			MicronodeGraphField micronodeGraphField = getMicronode(fieldKey);
			if (micronodeGraphField == null) {
				return Observable.just(new NullMicronodeResponse());
			} else {
				return micronodeGraphField.transformToRest(ac, fieldKey, languageTags);
			}
		}

		throw error(BAD_REQUEST, "type unknown");

	}

	/**
	 * Update or create the field using the given restField. The {@link FieldSchema} is used to determine the type of the field.
	 * 
	 * @param ac
	 *            Context of the request
	 * @param key
	 *            Key of the field
	 * @param restField
	 *            Rest model with data to be stored
	 * @param fieldSchema
	 *            Field schema of the field
	 * @param schema
	 */
	protected void updateField(InternalActionContext ac, String key, Field restField, FieldSchema fieldSchema, FieldSchemaContainer schema) {

		BootstrapInitializer boot = BootstrapInitializer.getBoot();

		FieldTypes type = FieldTypes.valueByName(fieldSchema.getType());
		switch (type) {
		case HTML:
			HtmlGraphField htmlGraphField = getHtml(key);
			failOnMissingMandatoryField(ac, htmlGraphField, restField, fieldSchema, key, schema);
			HtmlField htmlField = (HtmlFieldImpl) restField;
			if (restField == null) {
				return;
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
			failOnMissingMandatoryField(ac, graphStringField, restField, fieldSchema, key, schema);
			StringField stringField = (StringFieldImpl) restField;
			if (restField == null) {
				return;
			}

			// check value restrictions
			StringFieldSchema stringFieldSchema = (StringFieldSchema) fieldSchema;
			if (stringFieldSchema.getAllowedValues() != null) {
				if (stringField.getString() != null && !Arrays.asList(stringFieldSchema.getAllowedValues()).contains(stringField.getString())) {
					throw error(BAD_REQUEST, "node_error_invalid_string_field_value", key, stringField.getString());
				}
			}

			// Create new graph field if no existing one could be found
			if (graphStringField == null) {
				graphStringField = createString(key);
			}
			graphStringField.setString(stringField.getString());

			break;
		case BINARY:
			BinaryGraphField graphBinaryField = getBinary(key);
			failOnMissingMandatoryField(ac, graphBinaryField, restField, fieldSchema, key, schema);

			BinaryField binaryField = (BinaryFieldImpl) restField;
			if (restField == null) {
				return;
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
			failOnMissingMandatoryField(ac, numberGraphField, restField, fieldSchema, key, schema);
			NumberField numberField = (NumberFieldImpl) restField;
			if (restField == null) {
				return;
			}
			if (numberGraphField == null) {
				createNumber(key).setNumber(numberField.getNumber());
			} else {
				numberGraphField.setNumber(numberField.getNumber());
			}
			break;
		case BOOLEAN:
			BooleanGraphField booleanGraphField = getBoolean(key);
			failOnMissingMandatoryField(ac, booleanGraphField, restField, fieldSchema, key, schema);
			BooleanField booleanField = (BooleanFieldImpl) restField;
			if (restField == null) {
				return;
			}
			if (booleanGraphField == null) {
				createBoolean(key).setBoolean(booleanField.getValue());
			} else {
				booleanGraphField.setBoolean(booleanField.getValue());
			}
			break;
		case DATE:
			DateGraphField dateGraphField = getDate(key);
			failOnMissingMandatoryField(ac, dateGraphField, restField, fieldSchema, key, schema);
			DateField dateField = (DateFieldImpl) restField;
			if (restField == null) {
				return;
			}
			if (dateGraphField == null) {
				createDate(key).setDate(dateField.getDate());
			} else {
				dateGraphField.setDate(dateField.getDate());
			}
			break;
		case NODE:
			NodeGraphField graphNodeField = getNode(key);
			failOnMissingMandatoryField(ac, graphNodeField, restField, fieldSchema, key, schema);
			NodeField nodeField = (NodeField) restField;
			if (restField == null) {
				return;
			}
			Observable<Node> obsNode = boot.nodeRoot().findByUuid(nodeField.getUuid());
			obsNode.map(node -> {
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
				return null;
			}).toBlocking().single();
			break;
		case LIST:
			if (restField instanceof NodeFieldListImpl) {
				NodeGraphFieldList graphNodeFieldList = getNodeList(key);
				failOnMissingMandatoryField(ac, graphNodeFieldList, restField, fieldSchema, key, schema);
				NodeFieldListImpl nodeList = (NodeFieldListImpl) restField;

				if (nodeList.getItems().isEmpty()) {
					if (graphNodeFieldList != null) {
						graphNodeFieldList.removeField();
					}
				} else {
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
				}
			} else if (restField instanceof StringFieldListImpl) {
				StringGraphFieldList graphStringList = getStringList(key);
				failOnMissingMandatoryField(ac, graphStringList, restField, fieldSchema, key, schema);
				StringFieldListImpl stringList = (StringFieldListImpl) restField;

				if (stringList.getItems().isEmpty()) {
					if (graphStringList != null) {
						graphStringList.removeField();
					}
				} else {
					if (graphStringList == null) {
						graphStringList = createStringList(key);
					} else {
						graphStringList.removeAll();
					}
					for (String item : stringList.getItems()) {
						graphStringList.createString(item);
					}
				}

			} else if (restField instanceof HtmlFieldListImpl) {
				HtmlGraphFieldList graphHtmlFieldList = getHTMLList(key);
				failOnMissingMandatoryField(ac, graphHtmlFieldList, restField, fieldSchema, key, schema);
				HtmlFieldListImpl htmlList = (HtmlFieldListImpl) restField;

				if (htmlList.getItems().isEmpty()) {
					if (graphHtmlFieldList != null) {
						graphHtmlFieldList.removeField();
					}
				} else {
					if (graphHtmlFieldList == null) {
						graphHtmlFieldList = createHTMLList(key);
					} else {
						graphHtmlFieldList.removeAll();
					}
					for (String item : htmlList.getItems()) {
						graphHtmlFieldList.createHTML(item);
					}
				}
			} else if (restField instanceof NumberFieldListImpl) {
				NumberGraphFieldList graphNumberFieldList = getNumberList(key);
				failOnMissingMandatoryField(ac, graphNumberFieldList, restField, fieldSchema, key, schema);
				NumberFieldListImpl numberList = (NumberFieldListImpl) restField;

				if (numberList.getItems().isEmpty()) {
					if (graphNumberFieldList != null) {
						graphNumberFieldList.removeField();
					}
				} else {
					if (graphNumberFieldList == null) {
						graphNumberFieldList = createNumberList(key);
					} else {
						graphNumberFieldList.removeAll();
					}
					for (Number item : numberList.getItems()) {
						graphNumberFieldList.createNumber(item);
					}
				}
			} else if (restField instanceof BooleanFieldListImpl) {
				BooleanGraphFieldList graphBooleanFieldList = getBooleanList(key);
				failOnMissingMandatoryField(ac, graphBooleanFieldList, restField, fieldSchema, key, schema);
				BooleanFieldListImpl booleanList = (BooleanFieldListImpl) restField;

				if (booleanList.getItems().isEmpty()) {
					if (graphBooleanFieldList != null) {
						graphBooleanFieldList.removeField();
					}
				} else {
					if (graphBooleanFieldList == null) {
						graphBooleanFieldList = createBooleanList(key);
					} else {
						graphBooleanFieldList.removeAll();
					}
					for (Boolean item : booleanList.getItems()) {
						graphBooleanFieldList.createBoolean(item);
					}
				}
			} else if (restField instanceof DateFieldListImpl) {

				DateGraphFieldList graphDateFieldList = getDateList(key);
				failOnMissingMandatoryField(ac, graphDateFieldList, restField, fieldSchema, key, schema);
				DateFieldListImpl dateList = (DateFieldListImpl) restField;

				if (dateList.getItems().isEmpty()) {
					if (graphDateFieldList != null) {
						graphDateFieldList.removeField();
					}
				} else {
					// Create new list if no existing one could be found
					if (graphDateFieldList == null) {
						graphDateFieldList = createDateList(key);
					} else {
						graphDateFieldList.removeAll();
					}
					for (Long item : dateList.getItems()) {
						graphDateFieldList.createDate(item);
					}
				}
			} else if (restField instanceof MicronodeFieldListImpl) {
				MicronodeGraphFieldList micronodeGraphFieldList = getMicronodeList(key);
				failOnMissingMandatoryField(ac, micronodeGraphFieldList, restField, fieldSchema, key, schema);
				MicronodeFieldList micronodeList = (MicronodeFieldList) restField;

				if (micronodeList.getItems().isEmpty()) {
					if (micronodeGraphFieldList != null) {
						micronodeGraphFieldList.removeField();
					}
				} else {
					if (micronodeGraphFieldList == null) {
						micronodeGraphFieldList = createMicronodeFieldList(key);
					}

					//TODO instead this method should also return an observable 
					micronodeGraphFieldList.update(ac, micronodeList).toBlocking().last();
				}
			} else {
				if (restField == null) {
					return;
				} else {
					throw new NotImplementedException();
				}
			}
			break;
		case MICRONODE:
			MicronodeFieldSchema microschemaFieldSchema = (MicronodeFieldSchema) fieldSchema;
			failOnMissingMandatoryField(ac, getMicronode(key), restField, fieldSchema, key, schema);
			if (restField == null) {
				return;
			}
			MicronodeField micronodeField = (MicronodeField) restField;
			MicroschemaReference microschemaReference = micronodeField.getMicroschema();
			// TODO check for null
			if (microschemaReference == null) {
				return;
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
			if (microschemaContainer == null && !isEmpty(microschemaName)) {
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
				if ((!StringUtils.isEmpty(micronodeField.getUuid()) && !StringUtils.equalsIgnoreCase(micronode.getUuid(), micronodeField.getUuid()))
						|| !StringUtils.equalsIgnoreCase(microschemaContainer.getUuid(), existingContainer.getUuid())) {
					micronodeGraphField = createMicronode(key, microschemaContainer);
					micronode = micronodeGraphField.getMicronode();
				}
			}

			micronode.updateFieldsFromRest(ac, micronodeField.getFields(), micronode.getMicroschema());

			break;
		}
	}

	@Override
	public void updateFieldsFromRest(InternalActionContext ac, Map<String, Field> restFields, FieldSchemaContainer schema) {
		//TODO: This should return an observable
		// Initially all fields are not yet handled
		List<String> unhandledFieldKeys = new ArrayList<>(restFields.size());
		unhandledFieldKeys.addAll(restFields.keySet());

		// Iterate over all known field that are listed in the schema for the node
		for (FieldSchema entry : schema.getFields()) {
			String key = entry.getName();
			Field restField = restFields.get(key);
			unhandledFieldKeys.remove(key);
			updateField(ac, key, restField, entry, schema);
		}

		// Some fields were specified within the JSON but were not specified in the schema. Those fields can't be handled. We throw an error to inform the user
		// about this.
		String extraFields = "";
		for (String key : unhandledFieldKeys) {
			extraFields += "[" + key + "]";
		}

		if (!StringUtils.isEmpty(extraFields)) {
			throw error(BAD_REQUEST, "node_unhandled_fields", schema.getName(), extraFields);
		}

	}

	@Override
	public List<GraphField> getFields(FieldSchemaContainer schema) {
		List<GraphField> fields = new ArrayList<>();
		for (FieldSchema fieldSchema : schema.getFields()) {
			FieldTypes type = FieldTypes.valueByName(fieldSchema.getType());
			GraphField field = null;
			switch (type) {
			case BINARY:
				field = getBinary(fieldSchema.getName());
				break;
			case BOOLEAN:
				field = getBoolean(fieldSchema.getName());
				break;
			case DATE:
				field = getDate(fieldSchema.getName());
				break;
			case HTML:
				field = getHtml(fieldSchema.getName());
				break;
			case LIST:
				ListFieldSchema listFieldSchema = (ListFieldSchema)fieldSchema;
				switch (listFieldSchema.getListType()) {
				case "boolean":
					field = getBooleanList(fieldSchema.getName());
					break;
				case "date":
					field = getDateList(fieldSchema.getName());
					break;
				case "html":
					field = getHTMLList(fieldSchema.getName());
					break;
				case "micronode":
					field = getMicronodeList(fieldSchema.getName());
					break;
				case "node":
					field = getNodeList(fieldSchema.getName());
					break;
				case "number":
					field = getNumberList(fieldSchema.getName());
					break;
				case "string":
					field = getStringList(fieldSchema.getName());
					break;
				}
				break;
			case MICRONODE:
				field = getMicronode(fieldSchema.getName());
				break;
			case NODE:
				field = getNode(fieldSchema.getName());
				break;
			case NUMBER:
				field = getNumber(fieldSchema.getName());
				break;
			case STRING:
				field = getString(fieldSchema.getName());
				break;
			default:
				break;
			}

			if (field != null) {
				fields.add(field);
			}
		}
		return fields;
	}

	/**
	 * Delete the field with the given key from the container.
	 * 
	 * @param key
	 */
	private void deleteField(String key) {
		EdgeTraversal<?, ?, ?> traversal = outE(HAS_FIELD).has(GraphField.FIELD_KEY_PROPERTY_KEY, key);
		if (traversal.hasNext()) {
			traversal.next().remove();
		}
	}

	/**
	 * Throw an error if all assumptions match:
	 * <ul>
	 * <li>The given field has not yet been created</li>
	 * <li>The field is mandatory</li>
	 * <li>The rest field does not contain any data</li>
	 * </ul>
	 * 
	 * @param ac
	 * @param field
	 * @param restField
	 * @param fieldSchema
	 * @param key
	 * @param schema
	 * @throws HttpStatusCodeErrorException
	 */
	private void failOnMissingMandatoryField(InternalActionContext ac, GraphField field, Field restField, FieldSchema fieldSchema, String key,
			FieldSchemaContainer schema) throws HttpStatusCodeErrorException {
		if (field == null && fieldSchema.isRequired() && restField == null) {
			throw error(BAD_REQUEST, "node_error_missing_mandatory_field_value", key, schema.getName());
		}
	}

}
