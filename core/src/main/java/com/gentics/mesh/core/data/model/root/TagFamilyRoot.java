package com.gentics.mesh.core.data.model.root;

import static com.gentics.mesh.core.data.model.relationship.MeshRelationships.HAS_TAG;

import java.util.List;

import com.gentics.mesh.core.data.model.generic.MeshVertex;
import com.gentics.mesh.core.data.model.tinkerpop.Tag;

public class TagFamilyRoot extends MeshVertex {

	public List<? extends Tag> getTags() {
		return out(HAS_TAG).has(Tag.class).toListExplicit(Tag.class);
	}

}
