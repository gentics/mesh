package com.gentics.cailun.core.rest.model.relationship;

import org.springframework.data.neo4j.annotation.EndNode;
import org.springframework.data.neo4j.annotation.RelationshipEntity;
import org.springframework.data.neo4j.annotation.StartNode;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.gentics.cailun.core.rest.model.generic.AbstractPersistable;
import com.gentics.cailun.core.rest.model.generic.GenericNode;

@RelationshipEntity
public class Tagged extends AbstractPersistable {

	private static final long serialVersionUID = -3894271096711266689L;

	@JsonIgnore
	@StartNode
	private GenericNode startTag;

	@EndNode
	private GenericNode endTag;

	public Tagged(GenericNode startTag, GenericNode endTag) {
		this.startTag = startTag;
		this.endTag = endTag;
	}

	public Tagged() {
	}

	@JsonIgnore
	public GenericNode getStartTag() {
		return startTag;
	}

	@JsonIgnore
	public GenericNode getEndTag() {
		return endTag;
	}

}
