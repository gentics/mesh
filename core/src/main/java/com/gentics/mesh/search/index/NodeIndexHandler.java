package com.gentics.mesh.search.index;

import static com.gentics.mesh.search.index.MappingHelper.NAME_KEY;
import static com.gentics.mesh.search.index.MappingHelper.UUID_KEY;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.apache.commons.lang.NotImplementedException;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.admin.indices.mapping.put.PutMappingRequestBuilder;
import org.elasticsearch.action.admin.indices.mapping.put.PutMappingResponse;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.springframework.stereotype.Component;

import com.gentics.mesh.core.data.GraphFieldContainer;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.node.Micronode;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.field.BooleanGraphField;
import com.gentics.mesh.core.data.node.field.DateGraphField;
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
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.core.data.schema.MicroschemaContainerVersion;
import com.gentics.mesh.core.data.schema.SchemaContainerVersion;
import com.gentics.mesh.core.rest.common.FieldTypes;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.ListFieldSchema;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.impl.ListFieldSchemaImpl;
import com.gentics.mesh.etc.MeshSpringConfiguration;
import com.gentics.mesh.json.JsonUtil;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.rx.java.ObservableFuture;
import io.vertx.rx.java.RxHelper;
import rx.Observable;

/**
 * Handler for the node specific search index.
 */
@Component
public class NodeIndexHandler extends AbstractIndexHandler<Node> {

	private static final Logger log = LoggerFactory.getLogger(NodeIndexHandler.class);

	private static final String VERSION_KEY = "version";

	private static NodeIndexHandler instance;

	@PostConstruct
	public void setup() {
		NodeIndexHandler.instance = this;
	}

	public static NodeIndexHandler getInstance() {
		return instance;
	}

	/**
	 * Get the document type for documents stored for the given schema
	 * 
	 * @param schemaVersion
	 *            Schema container version
	 * @return document type
	 */
	public static String getDocumentType(SchemaContainerVersion schemaVersion) {
		return new StringBuilder(schemaVersion.getName()).append("-").append(schemaVersion.getVersion()).toString();
	}

	@Override
	protected String getIndex() {
		return Node.TYPE;
	}

	@Override
	protected String getType() {
		return Node.TYPE;
	}

	@Override
	protected RootVertex<Node> getRootVertex() {
		return boot.meshRoot().getNodeRoot();
	}

	@Override
	protected Map<String, Object> transformToDocumentMap(Node object) {
		throw new NotImplementedException("Nodes can't be directly transformed due to i18n support.");
	}

	@Override
	public Observable<Void> store(Node node, String type) {

		Map<String, Object> map = new HashMap<>();
		addBasicReferences(map, node);
		addProject(map, node.getProject());
		addTags(map, node.getTags());

		Set<Observable<Void>> obs = new HashSet<>();

		// The basenode has no parent.
		if (node.getParentNode() != null) {
			addParentNodeInfo(map, node.getParentNode());
		}
		for (NodeGraphFieldContainer container : node.getGraphFieldContainers()) {

			removeFieldEntries(map);
			map.remove("language");
			String language = container.getLanguage().getLanguageTag();
			map.put("language", language);
			addSchema(map, container.getSchemaContainerVersion());

			addFields(map, container, container.getSchemaContainerVersion().getSchema().getFields());
			if (log.isTraceEnabled()) {
				String json = JsonUtil.toJson(map);
				log.trace("Search index json:");
				log.trace(json);
			}

			// Add display field value
			Map<String, String> displayFieldMap = new HashMap<>();
			displayFieldMap.put("key", container.getSchemaContainerVersion().getSchema().getDisplayField());
			//			displayFieldMap.put("value", container.getDisplayFieldValue(container.getSchemaContainerVersion().getSchema()));
			map.put("displayField", displayFieldMap);
			obs.add(searchProvider.storeDocument(getIndex(), getDocumentType(container.getSchemaContainerVersion()),
					composeDocumentId(node, language), map));
		}

		return Observable.merge(obs).doOnCompleted(() -> {
			if (log.isDebugEnabled()) {
				log.debug("Stored node in index.");
			}
			MeshSpringConfiguration.getInstance().searchProvider().refreshIndex();
		});

	}

	@Override
	public Observable<Void> update(Node node) {

		Map<String, Object> map = new HashMap<>();
		addBasicReferences(map, node);

		addProject(map, node.getProject());
		addTags(map, node.getTags());

		Set<Observable<Void>> obs = new HashSet<>();
		for (NodeGraphFieldContainer container : node.getGraphFieldContainers()) {

			removeFieldEntries(map);
			map.remove("language");
			String language = container.getLanguage().getLanguageTag();
			map.put("language", language);
			addSchema(map, container.getSchemaContainerVersion());
			addFields(map, container, container.getSchemaContainerVersion().getSchema().getFields());
			if (log.isDebugEnabled()) {
				String json = JsonUtil.toJson(map);
				log.debug(json);
			}

			obs.add(searchProvider.updateDocument(getIndex(), getDocumentType(container.getSchemaContainerVersion()),
					composeDocumentId(node, language), map));

		}

		return Observable.merge(obs).doOnCompleted(() -> {
			if (log.isDebugEnabled()) {
				log.debug("Updated node in index.");
			}
			MeshSpringConfiguration.getInstance().searchProvider().refreshIndex();
		});
	}

	/**
	 * Add node fields to the given source map.
	 * 
	 * @param map
	 *            Search index document source map
	 * @param container
	 *            Node field container
	 * @param fields
	 *            List of schema fields that should be handled
	 */
	private void addFields(Map<String, Object> map, GraphFieldContainer container, List<? extends FieldSchema> fields) {
		Map<String, Object> fieldsMap = new HashMap<>();
		for (FieldSchema fieldSchema : fields) {
			String name = fieldSchema.getName();
			FieldTypes type = FieldTypes.valueByName(fieldSchema.getType());

			switch (type) {
			case STRING:
				StringGraphField stringField = container.getString(name);
				if (stringField != null) {
					fieldsMap.put(name, stringField.getString());
				}
				break;
			case HTML:
				HtmlGraphField htmlField = container.getHtml(name);
				if (htmlField != null) {
					fieldsMap.put(name, htmlField.getHTML());
				}
				break;
			case BOOLEAN:
				BooleanGraphField booleanField = container.getBoolean(name);
				if (booleanField != null) {
					fieldsMap.put(name, booleanField.getBoolean());
				}
				break;
			case DATE:
				DateGraphField dateField = container.getDate(name);
				if (dateField != null) {
					fieldsMap.put(name, dateField.getDate());
				}
				break;
			case NUMBER:
				NumberGraphField numberField = container.getNumber(name);
				if (numberField != null) {

					// Note: Lucene does not support BigDecimal/Decimal. It is not possible to store such values. ES will fallback to string in those cases.
					// The mesh json parser will not deserialize numbers into BigDecimal at this point. No need to check for big decimal is therefore needed.
					fieldsMap.put(name, numberField.getNumber());
				}
				break;
			case NODE:
				NodeGraphField nodeField = container.getNode(name);
				if (nodeField != null) {
					fieldsMap.put(name, nodeField.getNode().getUuid());
				}
				break;
			case LIST:
				if (fieldSchema instanceof ListFieldSchemaImpl) {
					ListFieldSchemaImpl listFieldSchema = (ListFieldSchemaImpl) fieldSchema;
					switch (listFieldSchema.getListType()) {
					case "node":
						NodeGraphFieldList graphNodeList = container.getNodeList(fieldSchema.getName());
						if (graphNodeList != null) {
							List<String> nodeItems = new ArrayList<>();
							for (NodeGraphField listItem : graphNodeList.getList()) {
								nodeItems.add(listItem.getNode().getUuid());
							}
							fieldsMap.put(fieldSchema.getName(), nodeItems);
						}
						break;
					case "date":
						DateGraphFieldList graphDateList = container.getDateList(fieldSchema.getName());
						if (graphDateList != null) {
							List<Long> dateItems = new ArrayList<>();
							for (DateGraphField listItem : graphDateList.getList()) {
								dateItems.add(listItem.getDate());
							}
							fieldsMap.put(fieldSchema.getName(), dateItems);
						}
						break;
					case "number":
						NumberGraphFieldList graphNumberList = container.getNumberList(fieldSchema.getName());
						if (graphNumberList != null) {
							List<Number> numberItems = new ArrayList<>();
							for (NumberGraphField listItem : graphNumberList.getList()) {
								// TODO Number can also be a big decimal. We need to convert those special objects into basic numbers or else ES will not be
								// able to store them
								numberItems.add(listItem.getNumber());
							}
							fieldsMap.put(fieldSchema.getName(), numberItems);
						}
						break;
					case "boolean":
						BooleanGraphFieldList graphBooleanList = container.getBooleanList(fieldSchema.getName());
						if (graphBooleanList != null) {
							List<String> booleanItems = new ArrayList<>();
							for (BooleanGraphField listItem : graphBooleanList.getList()) {
								booleanItems.add(String.valueOf(listItem.getBoolean()));
							}
							fieldsMap.put(fieldSchema.getName(), booleanItems);
						}
						break;
					case "micronode":
						MicronodeGraphFieldList micronodeGraphFieldList = container.getMicronodeList(fieldSchema.getName());
						if (micronodeGraphFieldList != null) {
							// add list of micronode objects
							fieldsMap.put(fieldSchema.getName(), Observable.from(micronodeGraphFieldList.getList()).map(item -> {
								Map<String, Object> itemMap = new HashMap<>();
								Micronode micronode = item.getMicronode();
								addMicroschema(itemMap, micronode.getMicroschemaContainerVersion());
								addFields(itemMap, micronode, micronode.getMicroschema().getFields());
								return itemMap;
							}).toList().toBlocking().first());
						}
						break;
					case "string":
						StringGraphFieldList graphStringList = container.getStringList(fieldSchema.getName());
						if (graphStringList != null) {
							List<String> stringItems = new ArrayList<>();
							for (StringGraphField listItem : graphStringList.getList()) {
								stringItems.add(listItem.getString());
							}
							fieldsMap.put(fieldSchema.getName(), stringItems);
						}
						break;
					case "html":
						HtmlGraphFieldList graphHtmlList = container.getHTMLList(fieldSchema.getName());
						if (graphHtmlList != null) {
							List<String> htmlItems = new ArrayList<>();
							for (HtmlGraphField listItem : graphHtmlList.getList()) {
								htmlItems.add(listItem.getHTML());
							}
							fieldsMap.put(fieldSchema.getName(), htmlItems);
						}
						break;
					default:
						log.error("Unknown list type {" + listFieldSchema.getListType() + "}");
						break;
					}
				}
				// container.getStringList(fieldKey)
				// ListField listField = container.getN(name);
				// fieldsMap.put(name, htmlField.getHTML());
				break;
			case MICRONODE:
				MicronodeGraphField micronodeGraphField = container.getMicronode(fieldSchema.getName());
				if (micronodeGraphField != null) {
					Micronode micronode = micronodeGraphField.getMicronode();
					if (micronode != null) {
						Map<String, Object> micronodeMap = new HashMap<>();
						addMicroschema(micronodeMap, micronode.getMicroschemaContainerVersion());
						addFields(micronodeMap, micronode, micronode.getMicroschema().getFields());
						fieldsMap.put(fieldSchema.getName(), micronodeMap);
					}
				}
				break;
			default:
				// TODO error?
				break;
			}

		}
		map.put("fields", fieldsMap);

	}

	/**
	 * Utility method that can be used to remove the <code>field.</code> field from the source map. This is useful when you want to just update the field values
	 * (eg. reuse the old sourcemap for a new sourcemap of a different language (same node)).
	 * 
	 * @param map
	 */
	private void removeFieldEntries(Map<String, Object> map) {
		for (String key : map.keySet()) {
			if (key.startsWith("field.")) {
				map.remove(key);
			}
		}
	}

	/**
	 * Transform the given schema and add it to the source map.
	 * 
	 * @param map
	 * @param schemaContainerVersion
	 */
	private void addSchema(Map<String, Object> map, SchemaContainerVersion schemaContainerVersion) {
		String name = schemaContainerVersion.getName();
		String uuid = schemaContainerVersion.getSchemaContainer().getUuid();
		Map<String, String> schemaFields = new HashMap<>();
		schemaFields.put(NAME_KEY, name);
		schemaFields.put(UUID_KEY, uuid);
		schemaFields.put(VERSION_KEY, String.valueOf(schemaContainerVersion.getVersion()));
		map.put("schema", schemaFields);
	}

	/**
	 * Transform the given microschema container and add it to the source map.
	 * 
	 * @param map
	 * @param microschemaContainerVersion
	 */
	private void addMicroschema(Map<String, Object> map, MicroschemaContainerVersion microschemaContainerVersion) {
		Map<String, String> microschemaFields = new HashMap<>();
		microschemaFields.put(NAME_KEY, microschemaContainerVersion.getName());
		microschemaFields.put(UUID_KEY, microschemaContainerVersion.getUuid());
		map.put("microschema", microschemaFields);
	}

	/**
	 * Use the given node to populate the parent node fields within the source map.
	 * 
	 * @param map
	 * @param parentNode
	 */
	private void addParentNodeInfo(Map<String, Object> map, Node parentNode) {
		Map<String, Object> parentNodeInfo = new HashMap<>();
		parentNodeInfo.put(UUID_KEY, parentNode.getUuid());
		// TODO check whether nesting of nested elements would also work
		//TODO FIXME MIGRATE: How to add this reference info? The schema is now linked to the node. Should we add another reference: (n:Node)->(sSchemaContainer) ?
		//		parentNodeInfo.put("schema.name", parentNode.getSchemaContainer().getName());
		//		parentNodeInfo.put("schema.uuid", parentNode.getSchemaContainer().getUuid());
		map.put("parentNode", parentNodeInfo);
	}

	/**
	 * Compose the document ID for the index document
	 * 
	 * @param node
	 *            node
	 * @param language
	 *            language
	 * @return document ID
	 */
	public static String composeDocumentId(Node node, String language) {
		StringBuilder id = new StringBuilder(node.getUuid());
		id.append("-").append(language);
		return id.toString();
	}

	/**
	 * Set the mapping for the given type in the given index for the schema
	 *
	 * @param indexName
	 *            index name
	 * @param type
	 *            type name
	 * @param schema
	 *            schema
	 * @return observable
	 */
	public Observable<Void> setNodeIndexMapping(String indexName, String type, Schema schema) {
		// Check whether the search provider is a dummy provider or not
		if (searchProvider.getNode() != null) {
			PutMappingRequestBuilder mappingRequestBuilder = searchProvider.getNode().client().admin().indices().preparePutMapping(indexName);
			mappingRequestBuilder.setType(type);

			try {
				XContentBuilder mappingBuilder = XContentFactory.jsonBuilder().startObject() // root object
						.startObject(type) // type
						.startObject("properties") // properties
						.startObject("fields") // fields
						.startObject("properties"); // properties

				for (FieldSchema field : schema.getFields()) {
					if (FieldTypes.valueByName(field.getType()) == FieldTypes.LIST) {
						if ("micronode".equals(((ListFieldSchema) field).getListType())) {
							mappingBuilder.startObject(field.getName()).field("type", "nested").endObject();
						}
					}
				}

				mappingBuilder.endObject() // properties
						.endObject() // fields
						.endObject() // properties
						.endObject() // type
						.endObject(); // root object
				if (log.isDebugEnabled()) {
					log.debug(mappingBuilder.string());
				}
				mappingRequestBuilder.setSource(mappingBuilder);

				ObservableFuture<Void> obs = RxHelper.observableFuture();
				mappingRequestBuilder.execute(new ActionListener<PutMappingResponse>() {

					@Override
					public void onResponse(PutMappingResponse response) {
						obs.toHandler().handle(Future.succeededFuture());
					}

					@Override
					public void onFailure(Throwable e) {
						obs.toHandler().handle(Future.failedFuture(e));
					}
				});
				return obs;
			} catch (Exception e) {
				return Observable.error(e);
			}
		} else {
			return Observable.just(null);
		}
	}

	@Override
	protected JsonObject getMapping() {
		JsonObject props = new JsonObject();

		return props;
	}

	//TODO Combine updateMapping with setNodeIndexMapping
	@Override
	public Observable<Void> init() {
		// Omit regular mapping creation for now.
		return createIndex();
	}

}
