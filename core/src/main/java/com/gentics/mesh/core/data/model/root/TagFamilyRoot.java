package com.gentics.mesh.core.data.model.root;

import com.gentics.mesh.core.data.model.generic.AbstractPersistable;
import com.gentics.mesh.core.data.model.relationship.BasicRelationships;
import com.gentics.mesh.core.data.model.tinkerpop.Tag;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.frames.Adjacency;

public interface TagFamilyRoot extends AbstractPersistable {

	@Adjacency(label = BasicRelationships.HAS_TAG, direction = Direction.OUT)
	public Iterable<Tag> getTags();

}
