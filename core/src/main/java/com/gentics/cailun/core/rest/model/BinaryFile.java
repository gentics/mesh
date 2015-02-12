package com.gentics.cailun.core.rest.model;

import org.springframework.data.neo4j.annotation.Fetch;

public class BinaryFile extends File {

	@Fetch
	protected String name;
}
