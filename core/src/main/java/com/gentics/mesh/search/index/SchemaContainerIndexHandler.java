package com.gentics.mesh.search.index;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.gentics.mesh.core.data.SchemaContainer;
import com.gentics.mesh.core.data.root.RootVertex;

@Component
public class SchemaContainerIndexHandler extends AbstractIndexHandler<SchemaContainer> {

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
		addBasicReferences(map, container);
		return map;
	}

}
