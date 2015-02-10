package com.gentics.cailun.core.rest.model;

import org.springframework.data.neo4j.annotation.EndNode;
import org.springframework.data.neo4j.annotation.RelationshipEntity;
import org.springframework.data.neo4j.annotation.StartNode;

import com.fasterxml.jackson.annotation.JsonIgnore;

@RelationshipEntity
public class Tagged extends AbstractPersistable {

	private static final long serialVersionUID = -3894271096711266689L;

	@JsonIgnore
	@StartNode
	private CaiLunNode startTag;

	@EndNode
	private CaiLunNode endTag;

	public Tagged(CaiLunNode startTag, CaiLunNode endTag) {
		this.startTag = startTag;
		this.endTag = endTag;
	}

	public Tagged() {
	}

	@JsonIgnore
	public CaiLunNode getStartTag() {
		return startTag;
	}

	@JsonIgnore
	public CaiLunNode getEndTag() {
		return endTag;
	}

}
