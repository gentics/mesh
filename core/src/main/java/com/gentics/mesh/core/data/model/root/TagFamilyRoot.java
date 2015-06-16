package com.gentics.mesh.core.data.model.root;

import java.util.List;

import com.gentics.mesh.core.data.model.generic.MeshVertex;
import com.gentics.mesh.core.data.model.relationship.MeshRelationships;
import com.gentics.mesh.core.data.model.tinkerpop.Tag;

public class TagFamilyRoot extends MeshVertex {

	public List<? extends Tag> getTags() {
		return out(MeshRelationships.HAS_TAG).toList(Tag.class);
	}

}
