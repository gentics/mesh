package com.gentics.mesh.search.index;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.gentics.mesh.core.data.SchemaContainer;

@Component
public class SchemaContainerIndexHandler extends AbstractIndexHandler<SchemaContainer> {

	@Override
	String getIndex() {
		return "schemaContainer";
	}

	@Override
	String getType() {
		return "schemaContainer";
	}

	@Override
	public void store(SchemaContainer container) {
		Map<String, Object> map = new HashMap<>();
		map.put("name", container.getName());
		addBasicReferences(map, container);
		store(container.getUuid(), map);
	}

	@Override
	public void store(String uuid) {
		boot.schemaContainerRoot().findByUuid(uuid, rh -> {
			if (rh.result() != null && rh.succeeded()) {
				SchemaContainer container = rh.result();
				store(container);
			} else {
				//TODO reply error? discard? log?
			}
		});
	}

	public void update(String uuid) {
		// TODO Auto-generated method stub
		
	}
}
