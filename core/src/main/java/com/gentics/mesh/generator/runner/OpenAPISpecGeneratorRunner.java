package com.gentics.mesh.generator.runner;

import java.io.File;

import com.gentics.mesh.generator.OpenAPIMockAPIGenerator;

public class OpenAPISpecGeneratorRunner {

	private static final long SPEC_AGE_MILLIS = 1000 * 60 * 5; 

	/**
	 * Start the generator.
	 * 
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		if (args == null || args.length < 1) {
			System.err.println("OpenAPISpecGeneratorRunner: no output folder provided");
			return;
		}
		final File outputFolder = new File(args[0]);

		if (!outputFolder.exists()) {
			outputFolder.mkdirs();
		} else {
			File openapiYaml = new File(outputFolder, "openapi.yaml");
			if (openapiYaml.exists() && (openapiYaml.lastModified() - System.currentTimeMillis()) < SPEC_AGE_MILLIS) {
				System.out.println("OpenAPISpecGeneratorRunner: openapi.yaml is fresh enough, no generation required");
				return;
			}
		}

		// Generate OpenAPI base spec
		OpenAPIMockAPIGenerator openApiGenerator = new OpenAPIMockAPIGenerator(outputFolder, "openapi.yaml", false);
		openApiGenerator.run();
	}
}
