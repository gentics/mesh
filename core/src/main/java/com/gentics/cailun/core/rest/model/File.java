package com.gentics.cailun.core.rest.model;

import lombok.NoArgsConstructor;

import org.antlr.v4.runtime.misc.NotNull;
import org.springframework.data.neo4j.annotation.Indexed;
import org.springframework.data.neo4j.annotation.NodeEntity;

@NodeEntity
@NoArgsConstructor
public class File extends TaggableNode {

	private static final long serialVersionUID = -8945772390192195270L;

	@Indexed
	@NotNull
	protected String filename;

	public String getFilename() {
		return filename;
	}

	public void setFilename(String filename) {
		this.filename = filename;
	}

}
