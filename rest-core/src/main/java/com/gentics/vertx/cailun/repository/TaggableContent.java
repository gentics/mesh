package com.gentics.vertx.cailun.repository;

import java.util.Collection;
import java.util.HashSet;

import lombok.Data;
import lombok.EqualsAndHashCode;

import org.neo4j.graphdb.Direction;
import org.springframework.data.neo4j.annotation.Fetch;
import org.springframework.data.neo4j.annotation.RelatedTo;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Data
@EqualsAndHashCode(callSuper = false)
abstract class TaggableContent extends AbstractPersistable {

	private static final long serialVersionUID = -4100747204275186859L;

	// @Fetch
	// @RelatedToVia(type = "TAGGED", direction = Direction.INCOMING, elementClass = Tagged.class)
	// private Collection<Tagged> parentTags = new HashSet<>();

	@Fetch
	@RelatedTo(type = "TAGGED", direction = Direction.OUTGOING, elementClass = Tag.class)
	private Collection<Tag> childTags = new HashSet<>();

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
		this.childTags.add(tag);
		return tag;
	}

	public Tag tag(Tag tag) {
		this.childTags.add(tag);
		return tag;
	}

	@JsonIgnore
	public Collection<Tag> getChildTags() {
		return childTags;
	}

	// public void addParentTag(String name) {
	// Tagged tagged = new Tagged(, endNode)
	// this.parentTags.add(new Tag(name));
	// }
}
