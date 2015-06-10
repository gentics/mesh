package com.gentics.mesh.core.data.model.root;

import java.util.List;

import com.gentics.mesh.core.data.model.generic.MeshVertex;
import com.gentics.mesh.core.data.model.relationship.BasicRelationships;
import com.gentics.mesh.core.data.model.tinkerpop.Language;

public class LanguageRoot extends MeshVertex {

	public List<Language> getLanguages() {
		return out(BasicRelationships.HAS_LANGUAGE).toList(Language.class);
	}

	public void addLanguage(Language language) {
		linkOut(language, BasicRelationships.HAS_LANGUAGE);
	}

	//TODO add unique index
}
