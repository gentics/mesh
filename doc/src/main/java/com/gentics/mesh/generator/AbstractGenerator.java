package com.gentics.mesh.generator;

import java.io.File;

import com.fasterxml.jackson.databind.ObjectMapper;

public abstract class AbstractGenerator {
	protected File outputDir;

	private ObjectMapper mapper = new ObjectMapper();

	public File getOutputDir() {
		return outputDir;
	}

	protected ObjectMapper getMapper() {
		return mapper;
	}
}
