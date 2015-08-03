package com.gentics.mesh.search.index;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.elasticsearch.action.index.IndexResponse;
import org.springframework.stereotype.Component;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.data.Language;
import com.gentics.mesh.core.data.NodeFieldContainer;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.SchemaContainer;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.field.basic.DateField;
import com.gentics.mesh.core.data.node.field.basic.HtmlField;
import com.gentics.mesh.core.data.node.field.basic.NumberField;
import com.gentics.mesh.core.data.node.field.basic.StringField;
import com.gentics.mesh.core.data.node.field.nesting.NodeField;
import com.gentics.mesh.core.rest.common.FieldTypes;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.Schema;

@Component
public class NodeIndexHandler extends AbstractIndexHandler<Node> {

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
		for (NodeFieldContainer container : node.getFieldContainers()) {
			removeFieldEntries(map);
			map.remove("language");
			String language = container.getLanguage().getLanguageTag();
			map.put("language", language);

			addFields(map, container, node.getSchema());
			//			String json = JsonUtil.toJson(map);
			//			System.out.println(json);
			store(node.getUuid(), map, getType() + "-" + language);
		}

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
			//			String json = JsonUtil.toJson(map);
			//			System.out.println(json);
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
				StringField stringField = container.getString(name);
				fieldsMap.put(name, stringField.getString());
				break;
			case HTML:
				HtmlField htmlField = container.getHtml(name);
				fieldsMap.put(name, htmlField.getHTML());
				break;
			case DATE:
				DateField dateField = container.getDate(name);
				fieldsMap.put(name, dateField.getDate());
				break;
			case NUMBER:
				NumberField numberField = container.getNumber(name);
				fieldsMap.put(name, numberField.getNumber());
				break;
			case NODE:
				NodeField nodeField = container.getNode(name);
				fieldsMap.put(name, nodeField.getNode());
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
		IndexResponse indexResponse = getClient().prepareIndex(getIndex(), type, uuid).setSource(map).execute().actionGet();
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
