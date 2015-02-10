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
public class Tag extends CaiLunNode {

	private static final long serialVersionUID = 3547707185082166132L;

	@Fetch
	@RelatedTo(type = BasicRelationships.TAGGED, direction = Direction.INCOMING, elementClass = CaiLunNode.class)
	private Set<CaiLunNode> contents = new HashSet<>();

	public Tag(String name) {
		setName(name);
	}

	/**
	 * Adds a content in the form of a generic node to this tag.
	 * 
	 * @param node
	 */
	public void addContent(CaiLunNode node) {
		this.contents.add(node);
	}

	@JsonIgnore
	public Set<CaiLunNode> getTaggedContents() {
		return contents;
	}

	public Set<CaiLunNode> getContents() {
		return contents;
	}

	public void setContents(Set<CaiLunNode> contents) {
		this.contents = contents;
	}

	/**
	 * Removes the given node from the list of contents.
	 * 
	 * @param node
	 * @return true, when the node could be removed. Otherwise false.
	 */
	public boolean removeContent(CaiLunNode node) {
		return this.contents.remove(node);
	}

}
