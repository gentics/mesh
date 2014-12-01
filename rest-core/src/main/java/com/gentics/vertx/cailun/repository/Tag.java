package com.gentics.vertx.cailun.repository;

import javax.validation.constraints.NotNull;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import org.springframework.data.neo4j.annotation.Indexed;
import org.springframework.data.neo4j.annotation.NodeEntity;

import com.fasterxml.jackson.annotation.JsonIgnore;

@NodeEntity
@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
public class Tag extends TaggableContent {

	private static final long serialVersionUID = 3547707185082166132L;

	@JsonIgnore
	@Indexed(unique = true)
	@NotNull
	String name;

	public Tag(String name) {
		this.name = name;
	}
	
	@JsonIgnore
	public String getName() {
		return name;
	}

}
