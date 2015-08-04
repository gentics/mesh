package com.gentics.mesh.search.index;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.data.TagFamily;

@Component
public class TagIndexHandler extends AbstractIndexHandler<Tag> {

	@Override
	String getIndex() {
		return "tag";
	}

	@Override
	String getType() {
		return "tag";
	}

	public void store(Tag tag) {
		Map<String, Object> map = new HashMap<>();
		Map<String, String> tagFields = new HashMap<>();
		tagFields.put("name", tag.getName());
		map.put("fields", tagFields);
		addBasicReferences(map, tag);
		addTagFamily(map, tag.getTagFamily());
		store(tag.getUuid(), map);
	}

	@Override
	public void store(String uuid) {
		boot.tagRoot().findByUuid(uuid, rh -> {
			if (rh.result() != null && rh.succeeded()) {
				Tag tag = rh.result();
				store(tag);
			} else {
				//TODO reply error? discard? log?
			}
		});

	}

	private void addTagFamily(Map<String, Object> map, TagFamily tagFamily) {
		Map<String, Object> tagFamilyFields = new HashMap<>();
		tagFamilyFields.put("name", tagFamily.getName());
		tagFamilyFields.put("uuid", tagFamily.getUuid());
		map.put("tagFamily", tagFamilyFields);
	}

	public void update(String uuid) {
		// TODO Auto-generated method stub
		
	}
}
