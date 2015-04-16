package com.gentics.cailun.core.data.model.relationship;

import org.springframework.data.neo4j.annotation.EndNode;
import org.springframework.data.neo4j.annotation.Fetch;
import org.springframework.data.neo4j.annotation.RelationshipEntity;
import org.springframework.data.neo4j.annotation.StartNode;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.gentics.cailun.core.data.model.Content;
import com.gentics.cailun.core.data.model.generic.AbstractPersistable;

@RelationshipEntity
public class Linked extends AbstractPersistable {

	private static final long serialVersionUID = -9078009095514379616L;

	@JsonIgnore
	@StartNode
	private Content startContent;

	@EndNode
	private Content endContent;

	public Linked(Content startPage, Content endPage) {
		this.startContent = startPage;
		this.endContent = endPage;
	}

	@JsonIgnore
	public Content getStartPage() {
		return startContent;
	}

	@JsonIgnore
	public Content getEndPage() {
		return endContent;
	}

	@JsonProperty("toId")
	public Long getEndPageId() {
		return endContent.getId();
	}

}
