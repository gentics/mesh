package com.gentics.mesh.core.data.model.tinkerpop;

import java.util.List;

import com.gentics.mesh.core.data.model.generic.MeshVertex;
import com.gentics.mesh.core.data.model.relationship.BasicRelationships;
import com.gentics.mesh.core.data.model.schema.propertytypes.PropertyType;

public class AbstractPropertyTypeSchema extends MeshVertex {

	private static final String TYPE_KEY = "type";
	private static final String DESCRIPTION_KEY = "description";
	private static final String KEY_KEY = "key";
	private static final String DISPLAY_NAME = "displayName";
	private static final String ORDER_KEY = "order";

	public List<? extends Translated> getI18nTranslations() {
		return outE(BasicRelationships.HAS_I18N_PROPERTIES).toList(Translated.class);
	}

	public String getType() {
		return getProperty(TYPE_KEY);
	}

	public void setType(PropertyType type) {
		setProperty(DESCRIPTION_KEY, type.getName());
	}

	public String getKey() {
		return getProperty(KEY_KEY);
	}

	public void setKey(String key) {
		setProperty(KEY_KEY, key);
	}

	public String getDescription() {
		return getProperty(DESCRIPTION_KEY);
	}

	public void setDescription(String description) {
		setProperty(DESCRIPTION_KEY, description);
	}

	public String getDisplayName() {
		return getProperty(DISPLAY_NAME);
	}

	public void setDisplayName(String displayName) {
		setProperty(DISPLAY_NAME, displayName);
	}

	public int getOrder() {
		return getProperty(ORDER_KEY);
	}

	public void setOrder(int order) {
		setProperty(ORDER_KEY, order);
	}

}
