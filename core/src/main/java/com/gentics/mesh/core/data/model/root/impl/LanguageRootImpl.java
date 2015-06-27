package com.gentics.mesh.core.data.model.root.impl;

import static com.gentics.mesh.core.data.model.relationship.MeshRelationships.HAS_LANGUAGE;

import java.util.List;

import com.gentics.mesh.core.data.model.Language;
import com.gentics.mesh.core.data.model.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.model.impl.LanguageImpl;
import com.gentics.mesh.core.data.model.root.LanguageRoot;

public class LanguageRootImpl extends MeshVertexImpl implements LanguageRoot {

	public List<? extends Language> getLanguages() {
		return out(HAS_LANGUAGE).toList(LanguageImpl.class);
	}

	@Override
	public void addLanguage(Language language) {
		linkOut((LanguageImpl) language, HAS_LANGUAGE);
	}

	// TODO add unique index

	public Language create(String languageName, String languageTag) {
		LanguageImpl language = getGraph().addFramedVertex(LanguageImpl.class);
		language.setName(languageName);
		language.setLanguageTag(languageTag);
		addLanguage(language);
		return language;
	}

	@Override
	public LanguageRootImpl getImpl() {
		return this;
	}
}
