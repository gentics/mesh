package com.gentics.mesh.core.data.model.schema;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.neo4j.graphdb.Direction;
import org.springframework.data.neo4j.annotation.Indexed;
import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.annotation.RelatedTo;
import org.springframework.data.neo4j.annotation.RelatedToVia;

import com.gentics.mesh.core.data.model.generic.GenericNode;
import com.gentics.mesh.core.data.model.relationship.BasicRelationships;
import com.gentics.mesh.core.data.model.relationship.Translated;
import com.gentics.mesh.core.data.model.schema.propertytypes.BasicPropertyTypeSchema;

/**
 * The object schema is used for validating CRUD actions and to provide a JSON schema that can be used for client side validation.
 * 
 * @author johannes2
 *
 */
@NodeEntity
public class ObjectSchema extends GenericNode {

	private static final long serialVersionUID = -6822013445735068604L;

	public static final String CONTENT_KEYWORD = "content";
	public static final String DISPLAY_NAME_KEYWORD = "displayName";
	public static final String NAME_KEYWORD = "name";
	public static final String TEASER_KEYWORD = "teaser";
	public static final String TITLE_KEYWORD = "title";

	private boolean isNestingAllowed = true;

	@Indexed(unique = true)
	private String name;

	private String displayName;
	// TODO i18n?
	private String description;

	@RelatedToVia(type = BasicRelationships.HAS_I18N_PROPERTIES, direction = Direction.OUTGOING, elementClass = Translated.class)
	protected Set<Translated> i18nTranslations = new HashSet<>();

	@RelatedTo(direction = Direction.OUTGOING, type = BasicRelationships.HAS_PROPERTY_TYPE_SCHEMA, elementClass = BasicPropertyTypeSchema.class)
	private Set<BasicPropertyTypeSchema> propertyTypeSchemas = new HashSet<>();

	@SuppressWarnings("unused")
	private ObjectSchema() {
	}

	public ObjectSchema(String name) {
		this.name = name;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getDescription() {
		return description;
	}

	public Set<BasicPropertyTypeSchema> getPropertyTypeSchemas() {
		return propertyTypeSchemas;
	}

	public BasicPropertyTypeSchema getPropertyTypeSchema(String typeKey) {
		if (StringUtils.isEmpty(typeKey)) {
			return null;
		}
		for (BasicPropertyTypeSchema propertyTypeSchema : propertyTypeSchemas) {
			if (propertyTypeSchema.getKey().equals(typeKey)) {
				return propertyTypeSchema;
			}
		}
		return null;
	}

	public void setPropertyTypeSchemas(Set<BasicPropertyTypeSchema> propertyTypeSchemas) {
		this.propertyTypeSchemas = propertyTypeSchemas;
	}

	public void addPropertyTypeSchema(BasicPropertyTypeSchema typeSchema) {
		this.propertyTypeSchemas.add(typeSchema);
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	public String getDisplayName() {
		return displayName;
	}

	public boolean isNestingAllowed() {
		return isNestingAllowed;
	}

	public void setNestingAllowed(boolean isNestingAllowed) {
		this.isNestingAllowed = isNestingAllowed;
	}

}
