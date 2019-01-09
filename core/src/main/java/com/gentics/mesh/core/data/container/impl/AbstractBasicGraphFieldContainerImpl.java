package com.gentics.mesh.core.data.container.impl;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_LANGUAGE;

import java.util.Map;

import com.gentics.mesh.core.data.BasicFieldContainer;
import com.gentics.mesh.core.data.Language;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.impl.LanguageImpl;

/**
 * Abstract implementation of a basic graph field container. This implementation will store basic graph fields within the properties of of this container
 * vertex.
 */
public abstract class AbstractBasicGraphFieldContainerImpl extends MeshVertexImpl implements BasicFieldContainer {

	public static final String I18N_PREFIX = "i18n-";

	@Override
	public String getLanguageTag() {
		return property("LANGUAGE_TAG");
	}

	@Override
	public void setLanguageTag(String languageTag) {
		property("LANGUAGE_TAG", languageTag);
	}

	/**
	 * Check whether the basic field with the given key and type exists in this field container.
	 * 
	 * @param key
	 * @param type
	 * @return
	 */
	public boolean fieldExists(String key, String type) {
		return getProperty(key + "-" + type) != null;
	}

	public Map<String, String> getI18nProperties() {
		return getProperties(I18N_PREFIX);
	}

	/**
	 * Return the i18n specific property for the given key.
	 * 
	 * @param key
	 * @return
	 */
	public String getI18nProperty(String key) {
		// TODO typecheck?
		return super.getProperty(I18N_PREFIX + key);
	}

	/**
	 * Set the i18n specific property for the given key.
	 * 
	 * @param key
	 * @param value
	 */
	public void setI18nProperty(String key, String value) {
		super.setProperty(I18N_PREFIX + key, value);
	}

	// TODO Check whether this is the correct way of dealing with property deletions
	public void removeI18nProperty(String key) {
		super.setProperty(I18N_PREFIX + key, null);
	}
}
