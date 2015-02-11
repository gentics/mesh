package com.gentics.cailun.core.rest.model;

import java.util.HashSet;
import java.util.Set;

import lombok.Data;

import org.codehaus.jackson.annotate.JsonIgnore;
import org.neo4j.graphdb.Direction;
import org.springframework.data.neo4j.annotation.Fetch;
import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.annotation.RelatedTo;

@Data
@NodeEntity
public class TaggableNode extends CaiLunNode {

	@JsonIgnore
	@Fetch
	@RelatedTo(type = BasicRelationships.TAGGED, direction = Direction.OUTGOING, elementClass = Tag.class)
	private Set<Tag> childTags = new HashSet<>();

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

}
