package com.gentics.mesh.core.data.model.root;

import static com.gentics.mesh.core.data.model.relationship.MeshRelationships.HAS_LANGUAGE;

import java.util.List;

import com.gentics.mesh.core.data.model.Language;
import com.gentics.mesh.core.data.model.generic.MeshVertex;

public class LanguageRoot extends MeshVertex {

	public List<? extends Language> getLanguages() {
		return out(HAS_LANGUAGE).toList(Language.class);
	}

	public void addLanguage(Language language) {
		linkOut(language, HAS_LANGUAGE);
	}

	// TODO add unique index

	public Language create(String languageName, String languageTag) {
		Language language = getGraph().addFramedVertex(Language.class);
		language.setName(languageName);
		language.setLanguageTag(languageTag);
		addLanguage(language);
		return language;
	}
}
