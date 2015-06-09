package com.gentics.mesh.core.data.model.root;

import java.util.List;

import com.gentics.mesh.core.data.model.generic.AbstractPersistable;
import com.gentics.mesh.core.data.model.relationship.BasicRelationships;
import com.gentics.mesh.core.data.model.tinkerpop.Language;

public class LanguageRoot extends AbstractPersistable {

//	@Adjacency(label = BasicRelationships.HAS_LANGUAGE, direction = Direction.OUT)
	public List<Language> getLanguages() {
		return out(BasicRelationships.HAS_LANGUAGE).toList(Language.class);
	}

//	@Adjacency(label = BasicRelationships.HAS_LANGUAGE, direction = Direction.OUT)
	public void addLanguage(Language language) {
		addEdge(BasicRelationships.HAS_LANGUAGE,  language, Language.class);
//		out(BasicRelationships.HAS_LANGUAGE).
	}

	//TODO add unique index
}
