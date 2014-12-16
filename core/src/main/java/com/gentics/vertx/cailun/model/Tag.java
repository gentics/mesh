package com.gentics.vertx.cailun.model;

import java.util.HashSet;
import java.util.Set;

import lombok.NoArgsConstructor;

import org.neo4j.graphdb.Direction;
import org.springframework.data.neo4j.annotation.Fetch;
import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.annotation.RelatedTo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.gentics.vertx.cailun.model.perm.Group;

@NodeEntity
@NoArgsConstructor
public class Tag extends GenericNode {

	private static final long serialVersionUID = 3547707185082166132L;

	@Fetch
	@RelatedTo(type = "TAGGED", direction = Direction.OUTGOING, elementClass = Tag.class)
	private Set<Tag> childTags = new HashSet<>();

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

	@Fetch
	// @Setter
	// @Getter(onMethod = @__({ @JsonIgnore }))
	@RelatedTo(type = "TAGGED", direction = Direction.INCOMING, elementClass = GenericNode.class)
	private Set<GenericNode> contents;

	// @Fetch
	// @RelatedToVia(type = "HAS_PERMISSION", direction = Direction.BOTH, elementClass = Permission.class)
	// private Set<Permission> permissions = new HashSet<Permission>();

	// @Getter(onMethod = @__({ @JsonIgnore }))
	// @Setter
	@Fetch
	@RelatedTo(type = "HAS_PERMISSION", direction = Direction.INCOMING, elementClass = Group.class)
	private Set<Group> assigned = new HashSet<>();

	public Tag(String name) {
		setName(name);
	}

	@JsonIgnore
	public Set<GenericNode> getTaggedContents() {
		return contents;
	}

	public Set<GenericNode> getContents() {
		return contents;
	}

	public void setContents(Set<GenericNode> contents) {
		this.contents = contents;
	}

	public Set<Group> getAssigned() {
		return assigned;
	}

	public void setAssigned(Set<Group> assigned) {
		this.assigned = assigned;
	}

	public void setChildTags(Set<Tag> childTags) {
		this.childTags = childTags;
	}

}
