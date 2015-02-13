//package com.gentics.cailun.core.rest.model.relationship;
//
//import lombok.Data;
//import lombok.EqualsAndHashCode;
//import lombok.NoArgsConstructor;
//
//import org.springframework.data.neo4j.annotation.EndNode;
//import org.springframework.data.neo4j.annotation.Fetch;
//import org.springframework.data.neo4j.annotation.RelationshipEntity;
//import org.springframework.data.neo4j.annotation.StartNode;
//
//import com.fasterxml.jackson.annotation.JsonIgnore;
//import com.fasterxml.jackson.annotation.JsonProperty;
//import com.gentics.cailun.core.rest.model.AbstractPersistable;
//import com.gentics.cailun.core.rest.model.Content;
//
//@RelationshipEntity
//@Data
//@EqualsAndHashCode(callSuper = false)
//@NoArgsConstructor
//public class Linked extends AbstractPersistable {
//
//	private static final long serialVersionUID = -9078009095514379616L;
//
//	@JsonIgnore
//	@StartNode
//	private Content startPage;
//
//	@Fetch
//	@EndNode
//	private Content endPage;
//
//	public Linked(Content startPage, Content endPage) {
//		this.startPage = startPage;
//		this.endPage = endPage;
//	}
//
//	@JsonIgnore
//	public Content getStartPage() {
//		return startPage;
//	}
//
//	@JsonIgnore
//	public Content getEndPage() {
//		return endPage;
//	}
//
//	@JsonProperty("toId")
//	public Long getEndPageId() {
//		return endPage.getId();
//	}
//
//}
