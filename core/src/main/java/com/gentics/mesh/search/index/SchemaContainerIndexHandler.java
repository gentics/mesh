package com.gentics.mesh.search.index;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.springframework.stereotype.Component;

import com.gentics.mesh.core.data.SchemaContainer;
import com.gentics.mesh.core.data.root.RootVertex;

@Component
public class SchemaContainerIndexHandler extends AbstractIndexHandler<SchemaContainer> {

	private static SchemaContainerIndexHandler instance;

	@PostConstruct
	public void setup() {
		instance = this;
	}

	public static SchemaContainerIndexHandler getInstance() {
		return instance;
	}

	
	@Override
	protected String getIndex() {
		return "schema_container";
	}

	@Override
	protected String getType() {
		return "schemaContainer";
	}

	@Override
	protected RootVertex<SchemaContainer> getRootVertex() {
		return boot.meshRoot().getSchemaContainerRoot();
	}

	@Override
	protected Map<String, Object> transformToDocumentMap(SchemaContainer container) {
		Map<String, Object> map = new HashMap<>();
		map.put("name", container.getName());
		map.put("description", container.getSchema().getDescription());
		addBasicReferences(map, container);
		return map;
	}

}
