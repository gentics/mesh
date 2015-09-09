package com.gentics.mesh.search.index;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.apache.commons.lang.NotImplementedException;
import org.elasticsearch.action.ActionResponse;
import org.springframework.stereotype.Component;

import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.SchemaContainer;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.field.basic.BooleanGraphField;
import com.gentics.mesh.core.data.node.field.basic.DateGraphField;
import com.gentics.mesh.core.data.node.field.basic.HtmlGraphField;
import com.gentics.mesh.core.data.node.field.basic.NumberGraphField;
import com.gentics.mesh.core.data.node.field.basic.StringGraphField;
import com.gentics.mesh.core.data.node.field.list.BooleanGraphFieldList;
import com.gentics.mesh.core.data.node.field.list.DateGraphFieldList;
import com.gentics.mesh.core.data.node.field.list.HtmlGraphFieldList;
import com.gentics.mesh.core.data.node.field.list.NumberGraphFieldList;
import com.gentics.mesh.core.data.node.field.list.StringGraphFieldList;
import com.gentics.mesh.core.data.node.field.nesting.NodeGraphField;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.core.rest.common.FieldTypes;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.impl.ListFieldSchemaImpl;
import com.gentics.mesh.etc.MeshSpringConfiguration;
import com.gentics.mesh.json.JsonUtil;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.rx.java.ObservableFuture;
import io.vertx.rx.java.RxHelper;
import rx.Observable;

@Component
public class NodeIndexHandler extends AbstractIndexHandler<Node> {

	private static final Logger log = LoggerFactory.getLogger(NodeIndexHandler.class);

	private static NodeIndexHandler instance;

	@PostConstruct
	public void setup() {
		NodeIndexHandler.instance = this;
	}

	public static NodeIndexHandler getInstance() {
		return instance;
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
	public void store(Node node, String type, Handler<AsyncResult<Void>> handler) {

		Map<String, Object> map = new HashMap<>();
		addBasicReferences(map, node);
		addSchema(map, node.getSchemaContainer());
		addProject(map, node.getProject());
		addTags(map, node.getTags());

		Set<ObservableFuture<Void>> futures = new HashSet<>();

		// The basenode has no parent.
		if (node.getParentNode() != null) {
			addParentNodeInfo(map, node.getParentNode());
		}
		for (NodeGraphFieldContainer container : node.getGraphFieldContainers()) {
			ObservableFuture<Void> obs = RxHelper.observableFuture();
			futures.add(obs);
			removeFieldEntries(map);
			map.remove("language");
			String language = container.getLanguage().getLanguageTag();
			map.put("language", language);

			addFields(map, container, node.getSchema());
			if (log.isDebugEnabled()) {
				String json = JsonUtil.toJson(map);
				log.debug(json);
			}
			searchProvider.storeDocument(getIndex(), getType() + "-" + language, node.getUuid(), map, obs.toHandler());
		}

		Observable.merge(futures).subscribe(item -> {
			if (log.isDebugEnabled()) {
				log.debug("Stored node in index.");
			}
		} , error -> {
			log.error("Error while storing node document.", error);
			handler.handle(Future.failedFuture(error));
		} , () -> {
			MeshSpringConfiguration.getMeshSpringConfiguration().searchProvider().refreshIndex();
			handler.handle(Future.succeededFuture());
		});

	}

	public void update(Node node, Handler<AsyncResult<ActionResponse>> handler) {

		Map<String, Object> map = new HashMap<>();
		addBasicReferences(map, node);
		addSchema(map, node.getSchemaContainer());
		addProject(map, node.getProject());
		addTags(map, node.getTags());

		Set<ObservableFuture<Void>> futures = new HashSet<>();
		for (NodeGraphFieldContainer container : node.getGraphFieldContainers()) {
			ObservableFuture<Void> obs = RxHelper.observableFuture();
			futures.add(obs);

			removeFieldEntries(map);
			map.remove("language");
			String language = container.getLanguage().getLanguageTag();
			map.put("language", language);

			addFields(map, container, node.getSchema());
			if (log.isDebugEnabled()) {
				String json = JsonUtil.toJson(map);
				log.debug(json);
			}
			searchProvider.updateDocument(getIndex(), getType() + "-" + language, node.getUuid(), map, obs.toHandler());
		}

		Observable.merge(futures).subscribe(item -> {
			if (log.isDebugEnabled()) {
				log.debug("Updated node in index.");
			}
		} , error -> {
			log.error("Error while updating node document.", error);
			handler.handle(Future.failedFuture(error));
		} , () -> {
			MeshSpringConfiguration.getMeshSpringConfiguration().searchProvider().refreshIndex();
			handler.handle(Future.succeededFuture());
		});
	}

	private void addFields(Map<String, Object> map, NodeGraphFieldContainer container, Schema schema) {
		Map<String, Object> fieldsMap = new HashMap<>();
		for (FieldSchema fieldSchema : schema.getFields()) {
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
			case DATE:
				DateGraphField dateField = container.getDate(name);
				if (dateField != null) {
					fieldsMap.put(name, dateField.getDate());
				}
				break;
			case NUMBER:
				NumberGraphField numberField = container.getNumber(name);
				if (numberField != null) {
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
						break;
					case "date":
						DateGraphFieldList graphDateList = container.getDateList(fieldSchema.getName());
						if (graphDateList != null) {
							List<String> dateItems = new ArrayList<>();
							for (DateGraphField listItem : graphDateList.getList()) {
								dateItems.add(listItem.getDate());
							}
							fieldsMap.put(fieldSchema.getName(), dateItems);
						}
						break;
					case "number":
						NumberGraphFieldList graphNumberList = container.getNumberList(fieldSchema.getName());
						if (graphNumberList != null) {
							List<String> numberItems = new ArrayList<>();
							for (NumberGraphField listItem : graphNumberList.getList()) {
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
					case "microschema":
						// TODO implement microschemas
						throw new NotImplementedException();
						// break;
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
			case SELECT:
				// TODO implement lists
				// break;
				throw new NotImplementedException();
			case MICROSCHEMA:
				// TODO implement microschemas
				// break;
				throw new NotImplementedException();
			default:
				// TODO error?
				break;
			}

		}
		map.put("fields", fieldsMap);

	}

	private void removeFieldEntries(Map<String, Object> map) {
		for (String key : map.keySet()) {
			if (key.startsWith("field.")) {
				map.remove(key);
			}
		}
	}

	private void addProject(Map<String, Object> map, Project project) {
		if (project != null) {
			Map<String, String> projectFields = new HashMap<>();
			projectFields.put("name", project.getName());
			projectFields.put("uuid", project.getUuid());
			map.put("project", projectFields);
		}
	}

	private void addSchema(Map<String, Object> map, SchemaContainer schemaContainer) {
		String name = schemaContainer.getName();
		String uuid = schemaContainer.getUuid();
		Map<String, String> schemaFields = new HashMap<>();
		schemaFields.put("name", name);
		schemaFields.put("uuid", uuid);
		map.put("schema", schemaFields);
	}

	private void addParentNodeInfo(Map<String, Object> map, Node parentNode) {
		Map<String, Object> parentNodeInfo = new HashMap<>();
		parentNodeInfo.put("uuid", parentNode.getUuid());
		//TODO check whether nesting of nested elements would also work
		parentNodeInfo.put("schema.name", parentNode.getSchemaContainer().getName());
		parentNodeInfo.put("schema.uuid", parentNode.getSchemaContainer().getUuid());
		map.put("parentNode", parentNodeInfo);
	}

}
