package com.gentics.cailun.core.rest.model;

import java.util.HashSet;
import java.util.Set;

import org.neo4j.graphdb.Direction;
import org.springframework.data.neo4j.annotation.Fetch;
import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.annotation.RelatedTo;
import org.springframework.data.neo4j.annotation.RelatedToVia;
import org.springframework.data.neo4j.fieldaccess.DynamicProperties;
import org.springframework.data.neo4j.fieldaccess.DynamicPropertiesContainer;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.gentics.cailun.core.rest.model.auth.AuthRelationships;
import com.gentics.cailun.core.rest.model.auth.GraphPermission;

/**
 * This class represents a basic cailun node. All models that make use of this model will automatically be able to be tagged and handled by the permission
 * system.
 * 
 * @author johannes2
 *
 */
@NodeEntity
public class CaiLunNode extends AbstractPersistable {

	private static final long serialVersionUID = -7525642021064006664L;

	@Fetch
	private String name;

	@Fetch
	@RelatedTo(type = BasicRelationships.TAGGED, direction = Direction.OUTGOING, elementClass = Tag.class)
	private Set<Tag> childTags = new HashSet<>();

	@RelatedToVia(type = AuthRelationships.HAS_PERMISSION, direction = Direction.INCOMING, elementClass = GraphPermission.class)
	private Set<GraphPermission> permissions = new HashSet<>();

	DynamicProperties properties = new DynamicPropertiesContainer();

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setChildTags(Set<Tag> childTags) {
		this.childTags = childTags;
	}

	public Tag tag(String name) {
		Tag tag = new Tag(name);
		this.childTags.add(tag);
		return tag;
	}

	public Tag tag(Tag tag) {
		this.childTags.add(tag);
		return tag;
	}

	public boolean unTag(String name) {
		return this.childTags.remove(new Tag(name));
	}

	public boolean unTag(Tag tag) {
		return this.childTags.remove(tag);
	}

	/**
	 * Check whether the node has the given child tag.
	 * 
	 * @param name
	 * @return true, when the node has the given tag. Otherwise false.
	 */
	public boolean hasTag(Tag tag) {
		return this.childTags.contains(tag);
	}

	@JsonIgnore
	public Set<Tag> getChildTags() {
		return this.childTags;
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

	/**
	 * Checks whether the property with the given key exists.
	 * 
	 * @param key
	 * @return true, when the key was found. Otherwise false.
	 */
	public boolean hasProperty(String key) {
		return properties.hasProperty(key);
	}

	public boolean addPermission(GraphPermission permission) {
		return permissions.add(permission);
	}

}