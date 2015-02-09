package com.gentics.cailun.core.rest.model;

import java.util.HashSet;
import java.util.Set;

import lombok.NoArgsConstructor;

import org.neo4j.graphdb.Direction;
import org.springframework.data.neo4j.annotation.Fetch;
import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.annotation.RelatedTo;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * A tag is a basic container for other generic nodes. It can be used as a folder that holds contents in the form of various pages.
 * 
 * @author johannes2
 *
 */
@NodeEntity
@NoArgsConstructor
public class Tag extends GenericNode {

	private static final long serialVersionUID = 3547707185082166132L;

	@Fetch
	@RelatedTo(type = BasicRelationships.TAGGED, direction = Direction.INCOMING, elementClass = GenericNode.class)
	private Set<GenericNode> contents = new HashSet<>();

	// @Fetch
	// @RelatedToVia(type = "HAS_PERMISSION", direction = Direction.BOTH, elementClass = Permission.class)
	// private Set<Permission> permissions = new HashSet<Permission>();

	public Tag(String name) {
		setName(name);
	}

	/**
	 * Adds a content in the form of a generic node to this tag.
	 * 
	 * @param node
	 */
	public void addContent(GenericNode node) {
		this.contents.add(node);
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

	/**
	 * Removes the given node from the list of contents.
	 * 
	 * @param node
	 * @return true, when the node could be removed. Otherwise false.
	 */
	public boolean removeContent(GenericNode node) {
		return this.contents.remove(node);
	}

}
