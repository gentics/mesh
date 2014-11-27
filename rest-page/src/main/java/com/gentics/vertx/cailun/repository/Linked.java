package com.gentics.vertx.cailun.repository;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import org.springframework.data.neo4j.annotation.EndNode;
import org.springframework.data.neo4j.annotation.Fetch;
import org.springframework.data.neo4j.annotation.RelationshipEntity;
import org.springframework.data.neo4j.annotation.StartNode;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

@RelationshipEntity
@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
public class Linked extends AbstractPersistable {

	private static final long serialVersionUID = -9078009095514379616L;

	@JsonIgnore
	// @Fetch
	@StartNode
	private Page startPage;

	// @JsonIgnore
	@Fetch
	@EndNode
	private Page endPage;

	public Linked(Page startPage, Page endPage) {
		this.startPage = startPage;
		this.endPage = endPage;
	}

	@JsonIgnore
	public Page getStartPage() {
		return startPage;
	}

	@JsonIgnore
	public Page getEndPage() {
		return endPage;
	}

	@JsonProperty("toId")
	public Long getEndPageId() {
		return endPage.getId();
	}

}
