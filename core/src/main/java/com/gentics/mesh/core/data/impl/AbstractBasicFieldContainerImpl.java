package com.gentics.mesh.core.data.impl;

import java.util.Map;

import com.gentics.mesh.core.data.BasicFieldContainer;
import com.gentics.mesh.core.data.Language;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.relationship.MeshRelationships;

public abstract class AbstractBasicFieldContainerImpl extends MeshVertexImpl implements BasicFieldContainer {

	public static final String I18N_PREFIX = "i18n-";

	@Override
	public Language getLanguage() {
		return out(MeshRelationships.HAS_LANGUAGE).nextOrDefault(LanguageImpl.class, null);
	}

	@Override
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
