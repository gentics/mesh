package com.gentics.mesh.core.data.model.tinkerpop;

import java.util.List;

import com.gentics.mesh.core.data.model.generic.GenericNode;
import com.gentics.mesh.core.data.model.relationship.BasicRelationships;
import com.gentics.mesh.core.data.model.schema.propertytypes.BasicPropertyType;

public class Schema extends GenericNode {

	public static final String CONTENT_KEYWORD = "content";
	public static final String DISPLAY_NAME_KEYWORD = "displayName";
	public static final String NAME_KEYWORD = "name";
	public static final String TEASER_KEYWORD = "teaser";
	public static final String TITLE_KEYWORD = "title";

	public boolean isNestingAllowed() {
		return getProperty("isNestingAllowed");
	}

	public void setNestingAllowed(boolean flag) {
		setProperty("isNestingAllowed", flag);
	}

	//TODO add unique index
	public String getName() {
		return getProperty("name");
	}

	public void setName(String name) {
		setProperty("name", name);
	}

	public String getDisplayName() {
		return getProperty("displayName");
	}

	public void setDisplayName(String name) {
		setProperty("displayName", name);
	}

	//TODO i18n?
	public String getDescription() {
		return getProperty("description");
	}

	public void setDescription(String description) {
		setProperty("description", description);
	}

	public List<Translated> getI18nTranslations() {
		return outE(BasicRelationships.HAS_I18N_PROPERTIES).toList(Translated.class);
	}

	public List<BasicPropertyType> getPropertyTypeSchemas() {
		return out(BasicRelationships.HAS_PROPERTY_TYPE_SCHEMA).toList(BasicPropertyType.class);
	}

//	@Adjacency(label = BasicRelationships.HAS_PROPERTY_TYPE_SCHEMA, direction = Direction.OUT)
	public void addPropertyTypeSchema(BasicPropertyType content) {
		
	}

}
