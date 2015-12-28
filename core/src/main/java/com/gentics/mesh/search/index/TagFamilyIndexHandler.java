package com.gentics.mesh.search.index;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.springframework.stereotype.Component;

import com.gentics.mesh.core.data.TagFamily;
import com.gentics.mesh.core.data.root.RootVertex;

@Component
public class TagFamilyIndexHandler extends AbstractIndexHandler<TagFamily> {

	private static TagFamilyIndexHandler instance;

	@PostConstruct
	public void setup() {
		instance = this;
	}

	public static TagFamilyIndexHandler getInstance() {
		return instance;
	}

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
		addTags(map, tagFamily.getTagRoot().findAll());
		addProject(map, tagFamily.getProject());
		return map;
	}

}
