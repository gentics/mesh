package com.gentics.mesh.core.data.model.impl;

import java.util.Map;

import com.gentics.mesh.core.data.model.BasicFieldContainer;
import com.gentics.mesh.core.data.model.Language;
import com.gentics.mesh.core.data.model.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.model.relationship.MeshRelationships;

public abstract class AbstractBasicFieldContainerImpl extends MeshVertexImpl implements BasicFieldContainer {

	public static final String I18N_PREFIX = "i18n-";

	public Language getLanguage() {
		return out(MeshRelationships.HAS_LANGUAGE).nextOrDefault(LanguageImpl.class, null);
	}

	public void setLanguage(Language language) {
		setLinkOut(language.getImpl(), MeshRelationships.HAS_LANGUAGE);
	}

	public boolean fieldExists(String key) {
		return getProperty(key + "-field") != null;
	}

	public Map<String, String> getI18nProperties() {
		return getProperties(I18N_PREFIX);
	}

	public String getI18nProperty(String key) {
		// TODO typecheck?
		return super.getProperty(I18N_PREFIX + key);
	}

	public void setI18nProperty(String key, String value) {
		super.setProperty(I18N_PREFIX + key, value);
	}

	// TODO Check whether this is the correct way of dealing with property deletions
	public void removeI18nProperty(String key) {
		super.setProperty(I18N_PREFIX + key, null);
	}
}
