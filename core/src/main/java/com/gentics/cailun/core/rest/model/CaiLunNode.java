package com.gentics.cailun.core.rest.model;

import java.util.HashSet;
import java.util.Set;

import lombok.Data;

import org.neo4j.graphdb.Direction;
import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.annotation.RelatedTo;
import org.springframework.data.neo4j.annotation.RelatedToVia;
import org.springframework.data.neo4j.fieldaccess.DynamicProperties;
import org.springframework.data.neo4j.fieldaccess.DynamicPropertiesContainer;

import com.gentics.cailun.core.rest.model.auth.AuthRelationships;
import com.gentics.cailun.core.rest.model.auth.GraphPermission;
import com.gentics.cailun.core.rest.model.auth.User;
import com.gentics.cailun.core.rest.model.relationship.BasicRelationships;

/**
 * This class represents a basic cailun node. All models that make use of this model will automatically be able to be handled by the permission system.
 * 
 * @author johannes2
 *
 */
@Data
@NodeEntity
public class CaiLunNode extends AbstractPersistable {

	private static final long serialVersionUID = -7525642021064006664L;

	@RelatedTo(type = BasicRelationships.ASSIGNED_TO_PROJECT, direction = Direction.OUTGOING, elementClass = Project.class)
	private Project project;

	@RelatedTo(type = BasicRelationships.HAS_CREATOR, direction = Direction.OUTGOING, elementClass = User.class)
	private User creator;
	
	@RelatedToVia(type = AuthRelationships.HAS_PERMISSION, direction = Direction.INCOMING, elementClass = GraphPermission.class)
	private Set<GraphPermission> permissions = new HashSet<>();

	DynamicProperties properties = new DynamicPropertiesContainer();

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