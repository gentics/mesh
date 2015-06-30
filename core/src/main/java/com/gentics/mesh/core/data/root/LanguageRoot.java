package com.gentics.mesh.core.data.root;

import java.util.List;

import com.gentics.mesh.core.data.Language;
import com.gentics.mesh.core.data.MeshVertex;
import com.gentics.mesh.core.data.root.impl.LanguageRootImpl;

public interface LanguageRoot extends MeshVertex {

	List<? extends Language> getLanguages();

	Language create(String languageName, String languageTag);

	void addLanguage(Language language);

	LanguageRootImpl getImpl();

}
