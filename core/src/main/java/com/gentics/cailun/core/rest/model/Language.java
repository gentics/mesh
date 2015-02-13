package com.gentics.cailun.core.rest.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import org.springframework.data.neo4j.annotation.Indexed;
import org.springframework.data.neo4j.annotation.NodeEntity;

@Data
@NodeEntity
@NoArgsConstructor
public class Language extends CaiLunNode {

	private static final long serialVersionUID = 8621659419142532208L;

	@Indexed
	protected String name;

	public Language(String name) {
		this.name = name;
	}

}
