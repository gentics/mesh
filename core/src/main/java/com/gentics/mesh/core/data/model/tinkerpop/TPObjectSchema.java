package com.gentics.mesh.core.data.model.tinkerpop;

import com.gentics.mesh.core.data.model.relationship.BasicRelationships;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.frames.Adjacency;

public interface TPObjectSchema extends TPGenericNode {

	@Adjacency(label = BasicRelationships.HAS_I18N_PROPERTIES, direction = Direction.OUT)
	public Iterable<TPTranslated> getI18nTranslations();

}
