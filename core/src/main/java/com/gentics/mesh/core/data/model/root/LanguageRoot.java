package com.gentics.mesh.core.data.model.root;

import java.util.List;

import com.gentics.mesh.core.data.model.generic.MeshVertex;
import com.gentics.mesh.core.data.model.relationship.MeshRelationships;
import com.gentics.mesh.core.data.model.tinkerpop.Language;

public class LanguageRoot extends MeshVertex {

	public List<? extends Language> getLanguages() {
		return out(MeshRelationships.HAS_LANGUAGE).toList(Language.class);
	}

	public void addLanguage(Language language) {
		linkOut(language, MeshRelationships.HAS_LANGUAGE);
	}

	// TODO add unique index
}
