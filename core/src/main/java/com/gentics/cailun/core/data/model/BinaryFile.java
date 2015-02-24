package com.gentics.cailun.core.data.model;

import org.springframework.data.neo4j.annotation.NodeEntity;

import com.gentics.cailun.core.data.model.generic.GenericFile;

@NodeEntity
public class BinaryFile extends GenericFile {

	private static final long serialVersionUID = -8420315613055842274L;

	public void setFilename(String filename) {

	}
}
