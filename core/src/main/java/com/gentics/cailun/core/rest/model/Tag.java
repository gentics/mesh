package com.gentics.cailun.core.rest.model;

import java.util.Set;

import lombok.NoArgsConstructor;

import org.neo4j.graphdb.Direction;
import org.springframework.data.neo4j.annotation.Fetch;
import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.annotation.RelatedTo;

import com.fasterxml.jackson.annotation.JsonIgnore;

@NodeEntity
@NoArgsConstructor
public class Tag extends GenericNode {

	private static final long serialVersionUID = 3547707185082166132L;

	@Fetch
	@RelatedTo(type = BasicRelationships.TAGGED, direction = Direction.INCOMING, elementClass = GenericNode.class)
	private Set<GenericNode> contents;

	// @Fetch
	// @RelatedToVia(type = "HAS_PERMISSION", direction = Direction.BOTH, elementClass = Permission.class)
	// private Set<Permission> permissions = new HashSet<Permission>();

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

}
