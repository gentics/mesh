package com.gentics.mesh.core.data.model.tinkerpop;

import com.gentics.mesh.core.data.model.Language;
import com.gentics.mesh.core.data.model.relationship.BasicRelationships;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.frames.Adjacency;

public interface TPLanguageRoot extends TPAbstractPersistable{

	@Adjacency(label = BasicRelationships.HAS_LANGUAGE, direction=Direction.OUT)
	public Iterable<Language> getLanguages();
}
