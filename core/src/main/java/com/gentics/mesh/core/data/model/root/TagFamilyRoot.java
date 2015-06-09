package com.gentics.mesh.core.data.model.root;

import java.util.List;

import com.gentics.mesh.core.data.model.generic.AbstractPersistable;
import com.gentics.mesh.core.data.model.relationship.BasicRelationships;
import com.gentics.mesh.core.data.model.tinkerpop.Tag;

public class TagFamilyRoot extends AbstractPersistable {

	public List<Tag> getTags() {
		return out(BasicRelationships.HAS_TAG).toList(Tag.class);
	}

}
