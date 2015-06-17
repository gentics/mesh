package com.gentics.mesh.core.data.model.tinkerpop;

import static com.gentics.mesh.util.TraversalHelper.nextOrNull;

import java.util.Map;

import com.gentics.mesh.core.data.model.generic.MeshVertex;
import com.gentics.mesh.core.data.model.relationship.MeshRelationships;

public class I18NProperties extends MeshVertex {

	public Language getLanguage() {
		return nextOrNull(out(MeshRelationships.HAS_LANGUAGE),Language.class);
	}

	public void setLanguage(Language language) {
		setLinkOut(language, MeshRelationships.HAS_LANGUAGE);
	}

	public Map<String, String> getProperties() {
		return getProperties("i18n-");
	}

	public String getProperty(String key) {
		//TODO typecheck?
		return super.getProperty("i18n-" + key);
	}

	public void setProperty(String key, String value) {
		super.setProperty("i18n-" + key, value);
	}

	//TODO Check whether this is the correct way of dealing with property deletions
	public void removeProperty(String key) {
		super.setProperty("i18n-" + key, null);
	}
}
