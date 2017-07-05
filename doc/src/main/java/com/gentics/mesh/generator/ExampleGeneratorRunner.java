package com.gentics.mesh.generator;

import java.io.File;

import com.gentics.mesh.OptionsLoader;

/**
 * Runner for various example generators.
 */
public class ExampleGeneratorRunner {

	public static File OUTPUT_ROOT_FOLDER = new File("src/main/docs/examples");

	public static void main(String[] args) throws Exception {
		if (!OUTPUT_ROOT_FOLDER.exists()) {
			OUTPUT_ROOT_FOLDER.mkdirs();
		}
		cleanConf();

		// Generate asciidoc tables to be included in the docs.
		TableGenerator tableGen = new TableGenerator(OUTPUT_ROOT_FOLDER);
		tableGen.run();

		// Generate model examples (json files)
		ModelExampleGenerator restModelGen = new ModelExampleGenerator(OUTPUT_ROOT_FOLDER);
		restModelGen.run();

		// Generate RAML
		RAMLGenerator generator = new RAMLGenerator(OUTPUT_ROOT_FOLDER);
		generator.run();

		// Generate elasticsearch flattened models
		SearchModelGenerator searchModelGen = new SearchModelGenerator(OUTPUT_ROOT_FOLDER);
		searchModelGen.run();
	}

	private static void cleanConf() {
		File conf = new File(OptionsLoader.MESH_CONF_FILENAME);
		if (conf.exists()) {
			conf.delete();
		}
	}

}
