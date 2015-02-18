package com.gentics.cailun.core.rest.model;

import org.springframework.data.neo4j.annotation.NodeEntity;

import com.gentics.cailun.core.rest.model.generic.GenericFile;
import com.gentics.cailun.core.rest.model.generic.GenericTag;

@NodeEntity
public class Tag extends GenericTag<Tag, GenericFile> {

	private static final long serialVersionUID = 7645315435657775862L;

	public Tag() {

	}

}
