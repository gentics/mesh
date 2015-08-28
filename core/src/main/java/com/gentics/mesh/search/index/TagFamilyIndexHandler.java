package com.gentics.mesh.search.index;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.gentics.mesh.core.data.TagFamily;
import com.gentics.mesh.core.data.root.RootVertex;

@Component
public class TagFamilyIndexHandler extends AbstractIndexHandler<TagFamily> {

	@Override
	protected String getIndex() {
		return "tag_family";
	}

	@Override
	protected String getType() {
		return "tagFamily";
	}

	@Override
	protected RootVertex<TagFamily> getRootVertex() {
		return boot.meshRoot().getTagFamilyRoot();
	}

	@Override
	protected Map<String, Object> transformToDocumentMap(TagFamily tagFamily) {
		Map<String, Object> map = new HashMap<>();
		map.put("name", tagFamily.getName());
		addBasicReferences(map, tagFamily);
		addTags(map, tagFamily.getTags());
		return map;
	}

}
