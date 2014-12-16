package com.gentics.vertx.cailun.model;

import java.util.HashSet;
import java.util.Set;

import javax.validation.constraints.NotNull;

import org.neo4j.graphdb.Direction;
import org.springframework.data.neo4j.annotation.Fetch;
import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.annotation.RelatedTo;

import com.fasterxml.jackson.annotation.JsonIgnore;

@NodeEntity
public class GenericNode extends AbstractPersistable {

	private static final long serialVersionUID = -7525642021064006664L;

	@Fetch
//	@Indexed(unique = true)
	@NotNull
	private String name;

	public String getName() {
		return name;
	}

	public void setChildTags(Set<Tag> childTags) {
		this.childTags = childTags;
	}

	public void setName(String name) {
		this.name = name;
	}

	// @Fetch
	// @RelatedToVia(type = "TAGGED", direction = Direction.INCOMING, elementClass = Tagged.class)
	// private Collection<Tagged> parentTags = new HashSet<>();

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

}