package com.gentics.mesh.core.data.model.tinkerpop;

import com.gentics.mesh.core.data.model.generic.GenericNode;
import com.gentics.mesh.core.data.model.relationship.BasicRelationships;
import com.gentics.mesh.core.data.model.schema.propertytypes.BasicPropertyTypeSchema;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.frames.Adjacency;
import com.tinkerpop.frames.Property;

public interface ObjectSchema extends GenericNode {

	public static final String CONTENT_KEYWORD = "content";
	public static final String DISPLAY_NAME_KEYWORD = "displayName";
	public static final String NAME_KEYWORD = "name";
	public static final String TEASER_KEYWORD = "teaser";
	public static final String TITLE_KEYWORD = "title";

	@Property("isNestingAllowed")
	public boolean isNestingAllowed();

	@Property("isNestingAllowed")
	public void setNestingAllowed(boolean flag);

	//TODO add unique index
	@Property("name")
	public String getName();

	@Property("name")
	public void setName(String name);

	@Property("displayName")
	public String getDisplayName();

	@Property("displayName")
	public void setDisplayName(String name);

	//TODO i18n?
	@Property("description")
	public String getDescription();

	@Property("description")
	public void setDescription(String description);

	@Adjacency(label = BasicRelationships.HAS_I18N_PROPERTIES, direction = Direction.OUT)
	public Iterable<Translated> getI18nTranslations();

	@Adjacency(label = BasicRelationships.HAS_PROPERTY_TYPE_SCHEMA, direction = Direction.OUT)
	public Iterable<BasicPropertyTypeSchema> getPropertyTypeSchemas();

	@Adjacency(label = BasicRelationships.HAS_PROPERTY_TYPE_SCHEMA, direction = Direction.OUT)
	public void addPropertyTypeSchema(BasicPropertyTypeSchema content);


}
