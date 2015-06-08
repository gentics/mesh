package com.gentics.mesh.core.data.model.root;

import com.gentics.mesh.core.data.model.generic.AbstractPersistable;
import com.gentics.mesh.core.data.model.relationship.BasicRelationships;
import com.gentics.mesh.core.data.model.tinkerpop.Language;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.frames.Adjacency;

public interface LanguageRoot extends AbstractPersistable {

	@Adjacency(label = BasicRelationships.HAS_LANGUAGE, direction = Direction.OUT)
	public Iterable<Language> getLanguages();

	@Adjacency(label = BasicRelationships.HAS_LANGUAGE, direction = Direction.OUT)
	public void addLanguage(Language language);

	//TODO add unique index
}
