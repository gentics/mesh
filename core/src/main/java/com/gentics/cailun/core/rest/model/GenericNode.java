package com.gentics.cailun.core.rest.model;

import java.util.HashSet;
import java.util.Set;

import org.neo4j.graphdb.Direction;
import org.springframework.data.neo4j.annotation.Fetch;
import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.annotation.RelatedTo;
import org.springframework.data.neo4j.annotation.RelatedToVia;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.gentics.cailun.core.rest.model.auth.AbstractPermissionRelationship;
import com.gentics.cailun.core.rest.model.auth.Role;

/**
 * This class represents a basic cailun node. All models that make use of this model will automatically be able to be tagged and handled by the permission
 * system.
 * 
 * @author johannes2
 *
 */
@NodeEntity
public class GenericNode extends AbstractPersistable {

	private static final long serialVersionUID = -7525642021064006664L;

	@Fetch
	private String name;

	@Fetch
	@RelatedTo(type = "TAGGED", direction = Direction.OUTGOING, elementClass = Tag.class)
	private Set<Tag> childTags = new HashSet<>();

	@Fetch
	@RelatedToVia(type = AbstractPermissionRelationship.RELATION_KEYWORD, direction = Direction.INCOMING, elementClass = AbstractPermissionRelationship.class)
	private Set<AbstractPermissionRelationship> permissions = new HashSet<>();

	@JsonIgnore
	public Set<? extends AbstractPermissionRelationship> getPermissions() {
		return permissions;
	}

	public void setPermissions(Set<AbstractPermissionRelationship> permissions) {
		this.permissions = permissions;
	}

	public void addPermissionSet(AbstractPermissionRelationship permissionSet) {
		this.permissions.add(permissionSet);
	}

	/**
	 * Adds a new permission set to this node.
	 * 
	 * @param role
	 * @return the created permissionset
	 */
	public AbstractPermissionRelationship addPermission(Role role) {
		AbstractPermissionRelationship perm = new AbstractPermissionRelationship(role, this);
		this.permissions.add(perm);
		return perm;
	}

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

	@JsonIgnore
	public Set<Tag> getChildTags() {
		return this.childTags;
	}

}