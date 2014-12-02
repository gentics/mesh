package com.gentics.vertx.cailun.repository;

import lombok.Data;
import lombok.EqualsAndHashCode;

import org.springframework.data.neo4j.annotation.EndNode;
import org.springframework.data.neo4j.annotation.RelationshipEntity;
import org.springframework.data.neo4j.annotation.StartNode;

import com.fasterxml.jackson.annotation.JsonIgnore;

@RelationshipEntity
@Data
@EqualsAndHashCode(callSuper = false)
public class Tagged extends AbstractPersistable {

	private static final long serialVersionUID = -3894271096711266689L;

	@JsonIgnore
	// @Fetch
	@StartNode
	private TaggableContent startTag;

	// @Fetch
	@EndNode
	private TaggableContent endTag;

	public Tagged(TaggableContent startTag, TaggableContent endTag) {
		this.startTag = startTag;
		this.endTag = endTag;
	}

	@JsonIgnore
	public TaggableContent getStartTag() {
		return startTag;
	}

	@JsonIgnore
	public TaggableContent getEndTag() {
		return endTag;
	}

}
