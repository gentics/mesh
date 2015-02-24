package com.gentics.cailun.core.data.model;

import org.springframework.data.neo4j.annotation.NodeEntity;

import com.gentics.cailun.core.data.model.generic.AbstractPersistable;

@NodeEntity
public class PropertyTypeSchema extends AbstractPersistable {

	private static final long serialVersionUID = 6242394504946538888L;

	PropertyType type;
	String key;
	// TODO i18n?
	String description;

	public PropertyTypeSchema(String key, PropertyType type) {
		this.key = key;
		this.type = type;
	}
	
	public PropertyTypeSchema() {
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

}
