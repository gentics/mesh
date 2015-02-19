package com.gentics.cailun.core.rest.model.relationship;

import org.springframework.data.neo4j.annotation.EndNode;
import org.springframework.data.neo4j.annotation.Fetch;
import org.springframework.data.neo4j.annotation.RelationshipEntity;
import org.springframework.data.neo4j.annotation.StartNode;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.gentics.cailun.core.rest.model.generic.AbstractPersistable;
import com.gentics.cailun.core.rest.model.generic.GenericContent;

@RelationshipEntity
public class Linked extends AbstractPersistable {

	private static final long serialVersionUID = -9078009095514379616L;

	@JsonIgnore
	@StartNode
	private GenericContent startPage;

	@Fetch
	@EndNode
	private GenericContent endPage;

	public Linked(GenericContent startPage, GenericContent endPage) {
		this.startPage = startPage;
		this.endPage = endPage;
	}

	@JsonIgnore
	public GenericContent getStartPage() {
		return startPage;
	}

	@JsonIgnore
	public GenericContent getEndPage() {
		return endPage;
	}

	@JsonProperty("toId")
	public Long getEndPageId() {
		return endPage.getId();
	}

}
