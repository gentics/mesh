package com.gentics.vertx.cailun.model;

import java.util.HashSet;
import java.util.Set;

import org.neo4j.graphdb.Direction;
import org.springframework.data.neo4j.annotation.Fetch;
import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.annotation.RelatedTo;

import com.fasterxml.jackson.annotation.JsonIgnore;

@NodeEntity
abstract class TaggableContent extends GenericNode {

	private static final long serialVersionUID = -4100747204275186859L;

	// @Fetch
	// @RelatedToVia(type = "TAGGED", direction = Direction.INCOMING, elementClass = Tagged.class)
	// private Collection<Tagged> parentTags = new HashSet<>();

	@Fetch
	@RelatedTo(type = "TAGGED", direction = Direction.OUTGOING, elementClass = Tag.class)
	private Set<Tag> childTags;

	// public void addChildTag(Tag tag) {
	// this.childTags.add(tag);
	// }
	//
	// public void addParentTag(Tag tag) {
	// this.parentTags.add(tag);
	// }
	//
	public Tag tag(String name) {
		Tag tag = new Tag(name);
		if (this.childTags == null) {
			this.childTags = new HashSet<>();
		}
		this.childTags.add(tag);
		return tag;
	}

	public Tag tag(Tag tag) {
		if (this.childTags == null) {
			this.childTags = new HashSet<>();
		}
		this.childTags.add(tag);
		return tag;
	}

	@JsonIgnore
	public Set<Tag> getChildTags() {
		if (this.childTags == null) {
			this.childTags = new HashSet<>();
		}
		return this.childTags;
	}

	// public void addParentTag(String name) {
	// Tagged tagged = new Tagged(, endNode)
	// this.parentTags.add(new Tag(name));
	// }
}
