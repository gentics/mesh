package com.gentics.mesh.core.data.service;

import java.util.List;

import org.springframework.stereotype.Component;

import com.gentics.mesh.core.data.TagFamily;
import com.gentics.mesh.core.data.impl.TagFamilyImpl;

@Component
public class TagFamilyService extends AbstractMeshGraphService<TagFamily> {

	@Override
	public TagFamily findByUUID(String uuid) {
		return fg.v().has("uuid", uuid).has(TagFamilyImpl.class).nextOrDefaultExplicit(TagFamilyImpl.class, null);
	}

	@Override
	public List<? extends TagFamily> findAll() {
		return fg.v().has(TagFamilyImpl.class).toListExplicit(TagFamilyImpl.class);
	}

}
