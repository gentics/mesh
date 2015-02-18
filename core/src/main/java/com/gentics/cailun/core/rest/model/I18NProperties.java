package com.gentics.cailun.core.rest.model;

import org.neo4j.graphdb.Direction;
import org.springframework.data.neo4j.annotation.Fetch;
import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.annotation.RelatedTo;
import org.springframework.data.neo4j.fieldaccess.DynamicProperties;
import org.springframework.data.neo4j.fieldaccess.DynamicPropertiesContainer;

import com.gentics.cailun.core.rest.model.generic.AbstractPersistable;
import com.gentics.cailun.core.rest.model.relationship.BasicRelationships;

@NodeEntity
public class I18NProperties extends AbstractPersistable {

	private static final long serialVersionUID = 7462760705716392395L;

	protected DynamicProperties properties = new DynamicPropertiesContainer();

	@Fetch
	@RelatedTo(type = BasicRelationships.HAS_LANGUAGE, direction = Direction.OUTGOING, elementClass = Language.class)
	private Language language;

	@SuppressWarnings("unused")
	private I18NProperties() {

	}

	public I18NProperties(Language language) {
		this.language = language;
	}

	public Language getLanguage() {
		return language;
	}

	public void setLanguage(Language language) {
		this.language = language;
	}

	/**
	 * Adds a new property or updates an exiting property with the given key and value.
	 * 
	 * @param key
	 * @param value
	 */
	public void addProperty(String key, String value) {
		this.properties.setProperty(key, value);
	}

	/**
	 * Returns the value for the given key.
	 * 
	 * @param key
	 * @return null, when the value could not be found
	 */
	public String getProperty(String key) {
		return (String) properties.getProperty(key);
	}

	/**
	 * Removes the property with the given key.
	 * 
	 * @param key
	 * @return true, when the property could be removed. Otherwise false.
	 */
	public boolean removeProperty(String key) {
		if (properties.removeProperty(key) == null) {
			return false;
		} else {
			return true;
		}
	}

}
