package com.gentics.mesh.search.index;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.elasticsearch.action.index.IndexResponse;
import org.springframework.stereotype.Component;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.cli.Mesh;
import com.gentics.mesh.core.data.Language;
import com.gentics.mesh.core.data.NodeFieldContainer;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.SchemaContainer;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.field.basic.DateGraphField;
import com.gentics.mesh.core.data.node.field.basic.HtmlGraphField;
import com.gentics.mesh.core.data.node.field.basic.NumberGraphField;
import com.gentics.mesh.core.data.node.field.basic.StringGraphField;
import com.gentics.mesh.core.data.node.field.nesting.GraphNodeField;
import com.gentics.mesh.core.rest.common.FieldTypes;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.search.SearchVerticle;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

@Component
public class NodeIndexHandler extends AbstractIndexHandler<Node> {

	private static final Logger log = LoggerFactory.getLogger(SearchVerticle.class);

	@Override
	String getIndex() {
		return "node";
	}

	@Override
	String getType() {
		return "node";
	}

	@Override
	public void store(Node node) throws IOException {
		Map<String, Object> map = new HashMap<>();
		addBasicReferences(map, node);
		addSchema(map, node.getSchemaContainer());
		addProject(map, node.getProject());
		addTags(map, node.getTags());
		addParentNodeInfo(map, node.getParentNode());
		for (NodeFieldContainer container : node.getFieldContainers()) {
			removeFieldEntries(map);
			map.remove("language");
			String language = container.getLanguage().getLanguageTag();
			map.put("language", language);

			addFields(map, container, node.getSchema());
			if (log.isDebugEnabled()) {
				String json = JsonUtil.toJson(map);
				log.debug(json);
			}
			store(node.getUuid(), map, getType() + "-" + language);
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
	public void update(String uuid) {
		boot.nodeRoot().findByUuid(uuid, rh -> {
			if (rh.result() != null && rh.succeeded()) {
				Node node = rh.result();
				try {
					update(node);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} else {
				//TODO reply error? discard? log?
			}
		});

	}

	private void update(Node node) throws IOException {

		Map<String, Object> map = new HashMap<>();
		addBasicReferences(map, node);
		addSchema(map, node.getSchemaContainer());
		addProject(map, node.getProject());
		addTags(map, node.getTags());
		for (NodeFieldContainer container : node.getFieldContainers()) {
			removeFieldEntries(map);
			map.remove("language");
			String language = container.getLanguage().getLanguageTag();
			map.put("language", language);

			addFields(map, container, node.getSchema());
			if (log.isDebugEnabled()) {
				String json = JsonUtil.toJson(map);
				log.debug(json);
			}
			update(node.getUuid(), map, getType() + "-" + language);
		}

	}

	@Override
	public void delete(String uuid) {
		//		DeleteResponse response = 
		for (Language lang : BootstrapInitializer.getBoot().languageRoot().findAll()) {
			String language = lang.getLanguageTag();
			getClient().prepareDelete(getIndex(), getType() + "-" + language, uuid).execute().actionGet();
		}
	}

	private void addFields(Map<String, Object> map, NodeFieldContainer container, Schema schema) {
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
					fieldsMap.put(name, nodeField.getNode());
				}
				break;
			case LIST:
				//ListField listField = container.getN(name);
				//fieldsMap.put(name, htmlField.getHTML());
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

	public void store(String uuid, Map<String, Object> map, String type) {
		Mesh.vertx().executeBlocking(bc -> {
			long start = System.currentTimeMillis();
			if (log.isDebugEnabled()) {
				log.debug("Adding object {" + uuid + ":" + type + "} to index.");
			}
			IndexResponse indexResponse = getClient().prepareIndex(getIndex(), type, uuid).setSource(map).execute().actionGet();
			if (log.isDebugEnabled()) {
				log.debug("Added object {" + uuid + ":" + type + "} to index. Duration " + (System.currentTimeMillis() - start) + "[ms]");
			}
		} , rh -> {

		});
	}

	@Override
	public void store(String uuid) {
		boot.nodeRoot().findByUuid(uuid, rh -> {
			if (rh.result() != null && rh.succeeded()) {
				Node node = rh.result();
				try {
					store(node);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} else {
				//TODO reply error? discard? log?
			}
		});
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
