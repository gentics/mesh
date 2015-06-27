package com.gentics.mesh.core.data.model.impl;

import java.util.Map;

import com.gentics.mesh.core.data.model.FieldContainer;
import com.gentics.mesh.core.data.model.Language;
import com.gentics.mesh.core.data.model.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.model.relationship.MeshRelationships;

public abstract class AbstractFieldContainerImpl extends MeshVertexImpl implements FieldContainer {

	public Language getLanguage() {
		return out(MeshRelationships.HAS_LANGUAGE).nextOrDefault(LanguageImpl.class, null);
	}

	public void setLanguage(Language language) {
		setLinkOut(language.getImpl(), MeshRelationships.HAS_LANGUAGE);
	}

	public Map<String, String> getProperties() {
		return getProperties("i18n-");
	}

	public String getProperty(String key) {
		// TODO typecheck?
		return super.getProperty("i18n-" + key);
	}

	public void setProperty(String key, String value) {
		super.setProperty("i18n-" + key, value);
	}

	// TODO Check whether this is the correct way of dealing with property deletions
	public void removeProperty(String key) {
		super.setProperty("i18n-" + key, null);
	}
}
