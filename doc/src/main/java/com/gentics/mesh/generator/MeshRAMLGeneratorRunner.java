package com.gentics.mesh.generator;

import java.io.File;

import org.apache.commons.io.FileUtils;

public class MeshRAMLGeneratorRunner {

	private static File outputFolder = new File("target", "api");

	public static void main(String[] args) throws Exception {
		if (outputFolder.exists()) {
			FileUtils.deleteDirectory(outputFolder);
		}
		RAMLGenerator generator = new RAMLGenerator(outputFolder);
		String raml = generator.generate();
		generator.writeFile("api.raml", raml);
	}
}
