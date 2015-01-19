package com.gentics.vertx.cailun.base.model;

import java.util.HashSet;
import java.util.Set;

import org.neo4j.graphdb.Direction;
import org.springframework.data.neo4j.annotation.Fetch;
import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.annotation.RelatedTo;
import org.springframework.data.neo4j.annotation.RelatedToVia;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.gentics.vertx.cailun.perm.model.Permission;
import com.gentics.vertx.cailun.perm.model.Role;
import com.gentics.vertx.cailun.tag.model.Tag;

@NodeEntity
public class GenericNode extends AbstractPersistable {

	private static final long serialVersionUID = -7525642021064006664L;

	@Fetch
	// @Indexed(unique = true)
	private String name;

	@Fetch
	@RelatedTo(type = "TAGGED", direction = Direction.OUTGOING, elementClass = Tag.class)
	private Set<Tag> childTags = new HashSet<>();

	// @Fetch
	// @RelatedToVia(type = "TAGGED", direction = Direction.INCOMING, elementClass = Tagged.class)
	// private Collection<Tagged> parentTags = new HashSet<>();

	@Fetch
	@RelatedToVia(type = "HAS_PERMISSION", direction = Direction.INCOMING, elementClass = Permission.class)
	private Set<Permission> permissions = new HashSet<>();

	public Set<Permission> getPermissions() {
		return permissions;
	}

	public void setPermissions(Set<Permission> permissions) {
		this.permissions = permissions;
	}

	public Permission addPermission(Role role) {
		Permission perm = new Permission(role, this);
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