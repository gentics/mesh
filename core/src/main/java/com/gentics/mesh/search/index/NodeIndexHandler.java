package com.gentics.mesh.search.index;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.ActionResponse;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.springframework.stereotype.Component;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.data.Language;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.SchemaContainer;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.field.basic.DateGraphField;
import com.gentics.mesh.core.data.node.field.basic.HtmlGraphField;
import com.gentics.mesh.core.data.node.field.basic.NumberGraphField;
import com.gentics.mesh.core.data.node.field.basic.StringGraphField;
import com.gentics.mesh.core.data.node.field.list.GraphDateFieldList;
import com.gentics.mesh.core.data.node.field.list.GraphHtmlFieldList;
import com.gentics.mesh.core.data.node.field.list.GraphNumberFieldList;
import com.gentics.mesh.core.data.node.field.list.GraphStringFieldList;
import com.gentics.mesh.core.data.node.field.nesting.GraphNodeField;
import com.gentics.mesh.core.rest.common.FieldTypes;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.impl.ListFieldSchemaImpl;
import com.gentics.mesh.graphdb.Trx;
import com.gentics.mesh.json.JsonUtil;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

@Component
public class NodeIndexHandler extends AbstractIndexHandler<Node> {

	private static final Logger log = LoggerFactory.getLogger(NodeIndexHandler.class);

	@Override
	String getIndex() {
		return "node";
	}

	@Override
	String getType() {
		return "node";
	}

	@Override
	public void store(Node node, Handler<AsyncResult<ActionResponse>> handler) throws IOException {

		Map<String, Object> map = new HashMap<>();
		addBasicReferences(map, node);
		addSchema(map, node.getSchemaContainer());
		addProject(map, node.getProject());
		addTags(map, node.getTags());

		// The basenode has no parent.
		if (node.getParentNode() != null) {
			addParentNodeInfo(map, node.getParentNode());
		}
		for (NodeGraphFieldContainer container : node.getFieldContainers()) {
			removeFieldEntries(map);
			map.remove("language");
			String language = container.getLanguage().getLanguageTag();
			map.put("language", language);

			addFields(map, container, node.getSchema());
			if (log.isDebugEnabled()) {
				String json = JsonUtil.toJson(map);
				log.debug(json);
			}
			store(node.getUuid(), map, getType() + "-" + language, handler);
		}

	}

	private void addParentNodeInfo(Map<String, Object> map, Node parentNode) {
		Map<String, Object> parentNodeInfo = new HashMap<>();
		parentNodeInfo.put("uuid", parentNode.getUuid());
		parentNodeInfo.put("schema.name", parentNode.getSchemaContainer().getName());
		parentNodeInfo.put("schema.uuid", parentNode.getSchemaContainer().getUuid());
		map.put("parent", parentNodeInfo);
	}

	@Override
	public void update(String uuid, Handler<AsyncResult<ActionResponse>> handler) {
		boot.nodeRoot().findByUuid(uuid, rh -> {
			if (rh.result() != null && rh.succeeded()) {
				Node node = rh.result();
				try {
					update(node, handler);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} else {
				// TODO reply error? discard? log?
			}
		});

	}

	private void update(Node node, Handler<AsyncResult<ActionResponse>> handler) throws IOException {

		Map<String, Object> map = new HashMap<>();
		addBasicReferences(map, node);
		addSchema(map, node.getSchemaContainer());
		addProject(map, node.getProject());
		addTags(map, node.getTags());
		for (NodeGraphFieldContainer container : node.getFieldContainers()) {
			removeFieldEntries(map);
			map.remove("language");
			String language = container.getLanguage().getLanguageTag();
			map.put("language", language);

			addFields(map, container, node.getSchema());
			if (log.isDebugEnabled()) {
				String json = JsonUtil.toJson(map);
				log.debug(json);
			}
			update(node.getUuid(), map, getType() + "-" + language, handler);
		}

	}

	@Override
	public void delete(String uuid, Handler<AsyncResult<ActionResponse>> handler) {
		for (Language lang : BootstrapInitializer.getBoot().languageRoot().findAll()) {
			String language = lang.getLanguageTag();
			getClient().prepareDelete(getIndex(), getType() + "-" + language, uuid).execute().addListener(new ActionListener<DeleteResponse>() {

				@Override
				public void onResponse(DeleteResponse response) {
					// TODO log
					handler.handle(Future.succeededFuture(response));
				}

				@Override
				public void onFailure(Throwable e) {
					// TODO log
					handler.handle(Future.failedFuture(e));
				}
			});
		}
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
				GraphNodeField nodeField = container.getNode(name);
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
						GraphDateFieldList graphDateList = container.getDateList(fieldSchema.getName());
						if (graphDateList != null) {
							List<String> dateItems = new ArrayList<>();
							for (DateGraphField listItem : graphDateList.getList()) {
								dateItems.add(listItem.getDate());
							}
							fieldsMap.put(fieldSchema.getName(), dateItems);
						}
						break;
					case "number":
						GraphNumberFieldList graphNumberList = container.getNumberList(fieldSchema.getName());
						if (graphNumberList != null) {
							List<String> numberItems = new ArrayList<>();
							for (NumberGraphField listItem : graphNumberList.getList()) {
								numberItems.add(listItem.getNumber());
							}
							fieldsMap.put(fieldSchema.getName(), numberItems);
						}
						break;
					case "boolean":
						break;
					case "microschema":
						break;
					case "string":
						GraphStringFieldList graphStringList = container.getStringList(fieldSchema.getName());
						if (graphStringList != null) {
							List<String> stringItems = new ArrayList<>();
							for (StringGraphField listItem : graphStringList.getList()) {
								stringItems.add(listItem.getString());
							}
							fieldsMap.put(fieldSchema.getName(), stringItems);
						}
						break;
					case "html":
						GraphHtmlFieldList graphHtmlList = container.getHTMLList(fieldSchema.getName());
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
				break;
			case MICROSCHEMA:
				break;
			default:
				break;
			}

		}
		map.put("fields", fieldsMap);

	}

	public void store(String uuid, Map<String, Object> map, String type, Handler<AsyncResult<ActionResponse>> handler) {
		long start = System.currentTimeMillis();
		if (log.isDebugEnabled()) {
			log.debug("Adding object {" + uuid + ":" + type + "} to index.");
		}
		getClient().prepareIndex(getIndex(), type, uuid).setSource(map).execute().addListener(new ActionListener<IndexResponse>() {

			@Override
			public void onResponse(IndexResponse response) {
				if (log.isDebugEnabled()) {
					log.debug("Added object {" + uuid + ":" + type + "} to index. Duration " + (System.currentTimeMillis() - start) + "[ms]");
				}
				handler.handle(Future.succeededFuture(response));

			}

			@Override
			public void onFailure(Throwable e) {
				if (log.isDebugEnabled()) {
					log.debug("Adding object {" + uuid + ":" + type + "} to index failed. Duration " + (System.currentTimeMillis() - start) + "[ms]");
				}
				handler.handle(Future.failedFuture(e));
			}
		});

	}

	@Override
	public void store(String uuid, Handler<AsyncResult<ActionResponse>> handler) {
		try (Trx tx = new Trx(db)) {
			boot.nodeRoot().findByUuid(uuid, rh -> {
				if (rh.result() != null && rh.succeeded()) {
					Node node = rh.result();
					try {
						store(node, handler);
					} catch (Exception e) {
						log.error("Error while storing node", e);
						handler.handle(Future.failedFuture(e));
					}
				} else {
					log.error("Could not find node {" + uuid + "}", rh.cause());
				}
			});
		}
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

}
