package com.gentics.cailun.core.rest.model;

import org.springframework.data.neo4j.annotation.NodeEntity;

@NodeEntity
public class FolderTag extends Tag<FolderTag, File> {

	private static final long serialVersionUID = 7645315435657775862L;

	public FolderTag() {

	}

}
