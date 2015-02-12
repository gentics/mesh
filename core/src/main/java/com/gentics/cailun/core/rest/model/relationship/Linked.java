package com.gentics.cailun.core.rest.model.relationship;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import org.springframework.data.neo4j.annotation.EndNode;
import org.springframework.data.neo4j.annotation.Fetch;
import org.springframework.data.neo4j.annotation.RelationshipEntity;
import org.springframework.data.neo4j.annotation.StartNode;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.gentics.cailun.core.rest.model.AbstractPersistable;
import com.gentics.cailun.core.rest.model.LocalizedContent;

@RelationshipEntity
@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
public class Linked extends AbstractPersistable {

	private static final long serialVersionUID = -9078009095514379616L;

	@JsonIgnore
	@StartNode
	private LocalizedContent startPage;

	@Fetch
	@EndNode
	private LocalizedContent endPage;

	public Linked(LocalizedContent startPage, LocalizedContent endPage) {
		this.startPage = startPage;
		this.endPage = endPage;
	}

	@JsonIgnore
	public LocalizedContent getStartPage() {
		return startPage;
	}

	@JsonIgnore
	public LocalizedContent getEndPage() {
		return endPage;
	}

	@JsonProperty("toId")
	public Long getEndPageId() {
		return endPage.getId();
	}

}
