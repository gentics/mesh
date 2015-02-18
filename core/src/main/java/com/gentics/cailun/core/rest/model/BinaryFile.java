package com.gentics.cailun.core.rest.model;

import org.springframework.data.neo4j.annotation.NodeEntity;

import com.gentics.cailun.core.rest.model.generic.GenericFile;

@NodeEntity
public class BinaryFile extends GenericFile {

	private static final long serialVersionUID = -8420315613055842274L;

	public void setFilename(String filename) {

	}
}
