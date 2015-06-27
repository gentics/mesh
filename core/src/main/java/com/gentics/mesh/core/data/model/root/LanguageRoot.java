package com.gentics.mesh.core.data.model.root;

import java.util.List;

import com.gentics.mesh.core.data.model.Language;
import com.gentics.mesh.core.data.model.MeshVertex;
import com.gentics.mesh.core.data.model.root.impl.LanguageRootImpl;

public interface LanguageRoot extends MeshVertex {

	List<? extends Language> getLanguages();

	Language create(String languageName, String languageTag);

	void addLanguage(Language language);

	LanguageRootImpl getImpl();

}
