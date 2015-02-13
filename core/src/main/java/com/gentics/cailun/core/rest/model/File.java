package com.gentics.cailun.core.rest.model;

import javax.validation.constraints.NotNull;

import lombok.NoArgsConstructor;

import org.springframework.data.neo4j.annotation.Indexed;
import org.springframework.data.neo4j.annotation.NodeEntity;

@NodeEntity
@NoArgsConstructor
public class File extends CaiLunNode {

	private static final long serialVersionUID = -8945772390192195270L;


	@Indexed
	@NotNull
	protected String filename;

}
