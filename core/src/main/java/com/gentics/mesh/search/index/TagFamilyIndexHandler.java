package com.gentics.mesh.search.index;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.gentics.mesh.core.data.TagFamily;

@Component
public class TagFamilyIndexHandler extends AbstractIndexHandler<TagFamily> {

	@Override
	String getIndex() {
		return "tag_family";
	}

	@Override
	String getType() {
		return "tagFamily";
	}

	@Override
	public void store(TagFamily tagFamily) {
		Map<String, Object> map = new HashMap<>();
		map.put("name", tagFamily.getName());
		addBasicReferences(map, tagFamily);
		addTags(map, tagFamily.getTags());
		store(tagFamily.getUuid(), map);
	}

	@Override
	public void store(String uuid) {
		boot.tagFamilyRoot().findByUuid(uuid, rh -> {
			if (rh.result() != null && rh.succeeded()) {
				TagFamily tagFamily = rh.result();
				store(tagFamily);
			} else {
				//TODO reply error? discard? log?
			}
		});

	}

	public void update(String uuid) {
		// TODO Auto-generated method stub
		
	}

}
