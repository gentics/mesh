package com.gentics.mesh.core.data.model.schema.propertytypes;

import java.util.HashSet;
import java.util.Set;

import org.neo4j.graphdb.Direction;
import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.annotation.RelatedToVia;

import com.gentics.mesh.core.data.model.generic.AbstractPersistable;
import com.gentics.mesh.core.data.model.relationship.BasicRelationships;
import com.gentics.mesh.core.data.model.relationship.Translated;

@NodeEntity
public abstract class AbstractPropertyTypeSchema extends AbstractPersistable {

	private static final long serialVersionUID = -2824172202337789322L;

	@RelatedToVia(type = BasicRelationships.HAS_I18N_PROPERTIES, direction = Direction.OUTGOING, elementClass = Translated.class)
	protected Set<Translated> i18nTranslations = new HashSet<>();

	private PropertyType type;
	private String key;
	// TODO i18n?
	private String description;
	private String displayName;
	private int order;

	public AbstractPropertyTypeSchema(String key, PropertyType type) {
		this.key = key;
		this.type = type;
	}

	public AbstractPropertyTypeSchema() {
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public PropertyType getType() {
		return type;
	}

	public void setType(PropertyType type) {
		this.type = type;
	}

	public int getOrder() {
		return order;
	}

	public void setOrder(int order) {
		this.order = order;
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

}
