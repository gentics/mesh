package com.gentics.cailun.core.rest.model;

import lombok.Data;

import org.springframework.data.neo4j.annotation.Fetch;
import org.springframework.data.neo4j.annotation.NodeEntity;

@NodeEntity
@Data
public class NamedCaiLunNode extends CaiLunNode {

	private static final long serialVersionUID = 707497196725566788L;

	@Fetch
	protected String name;

}
