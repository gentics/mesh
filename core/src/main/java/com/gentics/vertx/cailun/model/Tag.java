package com.gentics.vertx.cailun.model;

import java.util.Set;

import javax.validation.constraints.NotNull;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import org.neo4j.graphdb.Direction;
import org.springframework.data.neo4j.annotation.Fetch;
import org.springframework.data.neo4j.annotation.Indexed;
import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.annotation.RelatedTo;

import com.fasterxml.jackson.annotation.JsonIgnore;

@NodeEntity
@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
public class Tag extends TaggableContent {

	private static final long serialVersionUID = 3547707185082166132L;

	@Fetch
	@RelatedTo(type = "TAGGED", direction = Direction.INCOMING, elementClass = TaggableContent.class)
	private Set<TaggableContent> contents;

	@Fetch
	@Indexed(unique = true)
	@NotNull
	String name;

	public Tag(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	@JsonIgnore
	public Set<TaggableContent> getTaggedContents() {
		return contents;
	}

}
