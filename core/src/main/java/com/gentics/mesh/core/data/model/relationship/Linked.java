package com.gentics.mesh.core.data.model.relationship;

import org.springframework.data.neo4j.annotation.EndNode;
import org.springframework.data.neo4j.annotation.RelationshipEntity;
import org.springframework.data.neo4j.annotation.StartNode;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.gentics.mesh.core.data.model.MeshNode;
import com.gentics.mesh.core.data.model.generic.AbstractPersistable;

@RelationshipEntity
public class Linked extends AbstractPersistable {

	private static final long serialVersionUID = -9078009095514379616L;

	@JsonIgnore
	@StartNode
	private MeshNode startContent;

	@EndNode
	private MeshNode endContent;

	public Linked(MeshNode startPage, MeshNode endPage) {
		this.startContent = startPage;
		this.endContent = endPage;
	}

	@JsonIgnore
	public MeshNode getStartPage() {
		return startContent;
	}

	@JsonIgnore
	public MeshNode getEndPage() {
		return endContent;
	}

	@JsonProperty("toId")
	public Long getEndPageId() {
		return endContent.getId();
	}

}
