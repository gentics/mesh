package com.gentics.mesh.search.index;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.gentics.mesh.core.data.MicroschemaContainer;
import com.gentics.mesh.core.data.root.RootVertex;

@Component
public class MicroschemaContainerIndexHandler extends AbstractIndexHandler<MicroschemaContainer> {

	@Override
	protected String getIndex() {
		return "microschema";
	}

	@Override
	protected String getType() {
		return "microschema";
	}

	@Override
	protected RootVertex<MicroschemaContainer> getRootVertex() {
		return boot.meshRoot().getMicroschemaContainerRoot();
	}

	@Override
	protected Map<String, Object> transformToDocumentMap(MicroschemaContainer microschema) {
		Map<String, Object> map = new HashMap<>();
		addBasicReferences(map, microschema);
		map.put("name", microschema.getName());
		return map;
	}

}
