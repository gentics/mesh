package com.gentics.mesh.search.index;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.springframework.stereotype.Component;

import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.data.TagFamily;
import com.gentics.mesh.core.data.root.RootVertex;

@Component
public class TagIndexHandler extends AbstractIndexHandler<Tag> {

	private static TagIndexHandler instance;

	@PostConstruct
	public void setup() {
		instance = this;
	}

	public static TagIndexHandler getInstance() {
		return instance;
	}

	@Override
	protected String getIndex() {
		return Tag.TYPE;
	}

	@Override
	protected String getType() {
		return Tag.TYPE;
	}

	@Override
	protected RootVertex<Tag> getRootVertex() {
		return boot.meshRoot().getTagRoot();
	}

	@Override
	protected Map<String, Object> transformToDocumentMap(Tag tag) {
		Map<String, Object> map = new HashMap<>();
		Map<String, String> tagFields = new HashMap<>();
		tagFields.put("name", tag.getName());
		map.put("fields", tagFields);
		addBasicReferences(map, tag);
		addTagFamily(map, tag.getTagFamily());
		addProject(map, tag.getProject());
		return map;
	}

	private void addTagFamily(Map<String, Object> map, TagFamily tagFamily) {
		Map<String, Object> tagFamilyFields = new HashMap<>();
		tagFamilyFields.put("name", tagFamily.getName());
		tagFamilyFields.put("uuid", tagFamily.getUuid());
		map.put("tagFamily", tagFamilyFields);
	}

}
