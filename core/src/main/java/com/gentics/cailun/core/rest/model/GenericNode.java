package com.gentics.cailun.core.rest.model;

import java.util.HashSet;
import java.util.Set;

import org.neo4j.graphdb.Direction;
import org.springframework.data.neo4j.annotation.Fetch;
import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.annotation.RelatedTo;

import com.fasterxml.jackson.annotation.JsonIgnore;

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

//	@Fetch
//	@RelatedTo(type = AuthRelationships.HAS_PERMISSION, direction = Direction.OUTGOING, elementClass = AbstractPermission.class)
//	private Set<AbstractPermission> permissions = new HashSet<>();

//	@JsonIgnore
//	public Set<? extends AbstractPermission> getPermissions() {
//		return permissions;
//	}

//	public void addPermissionSet(AbstractPermission permission) {
//		this.permissions.add(permission);
//	}

//	/**
//	 * Adds a new permission set to this node.
//	 * 
//	 * @param role
//	 * @return the created permissionset
//	 */
//	public AbstractPermission addPermission(AbstractPermission permission) {
//		this.permissions.add(permission);
//		return permission;
//	}

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

//	public BasicPermission addBasicPermission(Role role) {
//		BasicPermission perm = new BasicPermission();
//		this.permissions.add(perm);
//		role.addPermission(perm);
//		return perm;
//	}

}